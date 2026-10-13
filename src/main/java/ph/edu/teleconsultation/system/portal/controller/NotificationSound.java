package ph.edu.teleconsultation.system.portal.controller;

import java.util.concurrent.ConcurrentHashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import ph.edu.teleconsultation.system.portal.model.Notification;

/**
 * Short chimes for notification pop-ups.
 *
 * The sounds are generated in code (a few sine-wave "bell" notes), so the
 * project needs no audio files and no extra libraries. Each notification type
 * has its own pattern: a soft two-note ding for information, a rising arpeggio
 * for success, a double beep for warnings and a three-note call for alerts.
 *
 * Why it used to stay silent: the old version played each chime through a
 * separate Clip that sat idle until needed. On many Windows sound devices an
 * idle line is put to sleep, so the first part of the chime was swallowed
 * while the device woke up (the whole short chime was gone), and one failed
 * start-up switched sound off until the app was restarted.
 *
 * Now one audio line is opened at start-up and kept running, streaming silence
 * between chimes. The device never goes to sleep, so a chime starts within
 * about 40 ms of the pop-up. If the audio device is missing or busy, the engine
 * tries again a few seconds later instead of giving up for good.
 *
 * If the computer really has no sound device, playing simply does nothing.
 */
public final class NotificationSound {

    private static final float SAMPLE_RATE = 44_100f;
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    /** Loudness from 0 to 1. Gentle, but loud enough to be heard on laptop speakers. */
    private static final double VOLUME = 0.40;

    /** Ignore a second chime that arrives right on top of the first. */
    private static final long MIN_GAP_NANOS = 250_000_000L;

    /** Line buffer: about 46 ms. This is also the most a chime can wait to start. */
    private static final int LINE_BUFFER_BYTES = 4096;

    /** Bytes written per loop (about 6 ms). */
    private static final int BLOCK_BYTES = 512;

    /** After a failure, wait this long before trying the audio device again. */
    private static final long RETRY_NANOS = 3_000_000_000L;

    private static final ConcurrentHashMap<Notification.Type, byte[]> CACHE =
            new ConcurrentHashMap<>();

    /** The chime being played right now and how far it has got. Guarded by LOCK. */
    private static final Object LOCK = new Object();
    private static byte[] voice;
    private static int voicePos;

    private static volatile boolean enabled = true;
    private static volatile Thread engine;
    private static volatile long retryAt;
    private static volatile long lastPlayed = System.nanoTime() - 2 * MIN_GAP_NANOS;

    private NotificationSound() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    /**
     * Builds every chime and opens the audio line in the background. Call once
     * when the app starts so the very first notification is already on time.
     */
    public static void warmUp() {
        ensureEngine();
    }

    /** Plays the chime for this kind of notification (if sound is on). */
    public static void play(Notification.Type type) {
        if (!enabled) {
            return;
        }

        long now = System.nanoTime();

        if (now - lastPlayed < MIN_GAP_NANOS) {
            return;
        }

        lastPlayed = now;

        Notification.Type kind = type == null ? Notification.Type.INFO : type;

        // Wakes the engine if it stopped (device was missing or busy earlier).
        ensureEngine();

        byte[] data = CACHE.computeIfAbsent(kind, NotificationSound::synthesize);

        synchronized (LOCK) {
            voice = data;
            voicePos = 0;
        }
    }

    // ------------------------------------------------------------------
    // Engine: one open line that never stops streaming
    // ------------------------------------------------------------------

    private static synchronized void ensureEngine() {
        Thread running = engine;

        if (running != null && running.isAlive()) {
            return;
        }

        if (System.nanoTime() < retryAt) {
            return;
        }

        Thread thread = new Thread(NotificationSound::runEngine, "notification-sound");
        thread.setDaemon(true);
        engine = thread;
        thread.start();
    }

    private static void runEngine() {
        SourceDataLine line = null;

        try {
            for (Notification.Type type : Notification.Type.values()) {
                CACHE.computeIfAbsent(type, NotificationSound::synthesize);
            }

            line = openLine();
            line.start();

            byte[] block = new byte[BLOCK_BYTES];

            while (line.isOpen()) {
                fill(block);
                line.write(block, 0, block.length);
            }

        } catch (Exception | LinkageError e) {
            // No speakers or the device is busy: stay quiet now, try again soon.
        } finally {
            if (line != null) {
                try {
                    line.close();
                } catch (Exception ignored) {
                    // nothing to do
                }
            }

            retryAt = System.nanoTime() + RETRY_NANOS;
            engine = null;
        }
    }

    /** Copies the next piece of the current chime into the block; silence when none is playing. */
    private static void fill(byte[] block) {
        java.util.Arrays.fill(block, (byte) 0);

        synchronized (LOCK) {
            if (voice == null) {
                return;
            }

            int count = Math.min(block.length, voice.length - voicePos);
            System.arraycopy(voice, voicePos, block, 0, count);
            voicePos += count;

            if (voicePos >= voice.length) {
                voice = null;
            }
        }
    }

    /** The default output line, or the first output device that accepts the format. */
    private static SourceDataLine openLine() throws LineUnavailableException {
        try {
            SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT);
            line.open(FORMAT, LINE_BUFFER_BYTES);
            return line;

        } catch (Exception | LinkageError first) {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);

            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                try {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);

                    if (mixer.isLineSupported(info)) {
                        SourceDataLine line = (SourceDataLine) mixer.getLine(info);
                        line.open(FORMAT, LINE_BUFFER_BYTES);
                        return line;
                    }
                } catch (Exception | LinkageError ignored) {
                    // try the next device
                }
            }

            throw new LineUnavailableException("No audio output available");
        }
    }

    /** Builds the 16-bit mono PCM samples for one type of chime. */
    static byte[] synthesize(Notification.Type type) {
        return switch (type) {
            case SUCCESS -> chime(0.09, 523.25, 659.25, 783.99);
            case WARNING -> chime(0.18, 587.33, 587.33);
            case ALERT   -> chime(0.12, 880.00, 659.25, 880.00);
            default      -> chime(0.11, 659.25, 880.00);
        };
    }

    /** Notes start {@code step} seconds apart and ring on top of each other like a bell. */
    private static byte[] chime(double step, double... notes) {
        double noteLength = 0.45;
        int total = (int) ((step * (notes.length - 1) + noteLength) * SAMPLE_RATE);
        double[] mix = new double[total];

        for (int n = 0; n < notes.length; n++) {
            int start = (int) (n * step * SAMPLE_RATE);
            int length = (int) (noteLength * SAMPLE_RATE);

            for (int i = 0; i < length && start + i < total; i++) {
                double t = i / (double) SAMPLE_RATE;
                double attack = Math.min(1.0, t / 0.006);   // 6 ms fade-in: no click
                double decay = Math.exp(-7.5 * t);           // bell-like fade-out
                double wave = Math.sin(2 * Math.PI * notes[n] * t)
                        + 0.30 * Math.sin(2 * Math.PI * notes[n] * 2 * t)
                        + 0.10 * Math.sin(2 * Math.PI * notes[n] * 3 * t);

                mix[start + i] += wave * attack * decay;
            }
        }

        double peak = 0.0001;

        for (double v : mix) {
            peak = Math.max(peak, Math.abs(v));
        }

        byte[] out = new byte[total * 2];

        for (int i = 0; i < total; i++) {
            short sample = (short) Math.round(mix[i] / peak * VOLUME * Short.MAX_VALUE);
            out[2 * i] = (byte) (sample & 0xFF);
            out[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        return out;
    }
}
