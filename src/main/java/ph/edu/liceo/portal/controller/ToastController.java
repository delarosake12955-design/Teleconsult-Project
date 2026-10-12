package ph.edu.liceo.portal.controller;

import javafx.animation.Interpolator;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import ph.edu.liceo.portal.model.Notification;

/** One notification pop-up: slides in from the right, counts down, slides out. */
public class ToastController {

    private static final double LIFETIME_SECONDS = 6;
    private static final double SLIDE_DISTANCE = 380;

    @FXML private VBox card;
    @FXML private StackPane bellPane;
    @FXML private Group bellSwing;
    @FXML private SVGPath waveLeftInner;
    @FXML private SVGPath waveLeftOuter;
    @FXML private SVGPath waveRightInner;
    @FXML private SVGPath waveRightOuter;
    @FXML private StackPane iconCircle;
    @FXML private Label iconLabel;
    @FXML private Label titleLabel;
    @FXML private Label timeLabel;
    @FXML private Label messageLabel;
    @FXML private Button closeButton;
    @FXML private ProgressBar timerBar;

    private Timeline countdown;
    private Timeline ringLoop;
    private Rotate swing;
    private boolean closing;

    @FXML
    private void initialize() {

        // The bell swings around the top of its handle.
        swing = new Rotate(0, 12, 2.6);
        bellSwing.getTransforms().add(swing);

        for (SVGPath wave : new SVGPath[] {
                waveLeftInner, waveLeftOuter, waveRightInner, waveRightOuter}) {
            wave.setOpacity(0);
        }
        // A click on the close button must not also count as a click on the card.
        closeButton.setOnMouseClicked(event -> event.consume());

        // Keep the toast on screen while the mouse is over it.
        card.setOnMouseEntered(event -> {
            if (countdown != null) {
                countdown.pause();
            }
        });

        card.setOnMouseExited(event -> {
            if (countdown != null && !closing) {
                countdown.play();
            }
        });
    }

    /** Fills the toast; {@code onOpen} runs when the person clicks the toast itself. */
    void configure(Notification notification, Runnable onOpen) {
        titleLabel.setText(notification.getTitle());
        messageLabel.setText(notification.getMessage());
        timeLabel.setText("just now");

        String type = notification.getType().name().toLowerCase();
        card.getStyleClass().add("toast-" + type);

        iconLabel.setText(switch (notification.getType()) {
            case SUCCESS -> "✓";
            case WARNING, ALERT -> "!";
            default -> "i";
        });

        card.setOnMouseClicked(event -> {
            if (onOpen != null) {
                onOpen.run();
            }
            dismiss();
        });
    }

    /** Slide in, then start the countdown. */
    void play() {
        card.setOpacity(0);
        card.setTranslateX(SLIDE_DISTANCE);

        TranslateTransition slide = new TranslateTransition(Duration.millis(340), card);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(260), card);
        fade.setToValue(1);

        new ParallelTransition(slide, fade).play();

        startBell();

        countdown = new Timeline(new KeyFrame(
                Duration.seconds(LIFETIME_SECONDS),
                new KeyValue(timerBar.progressProperty(), 0)));
        countdown.setOnFinished(event -> dismiss());
        countdown.play();
    }

    /**
     * The bell pops in with a little bounce, then rings: it swings left and
     * right with a shrinking angle while sound waves pulse out on both sides.
     * The first swing starts in the same instant as the chime; it rings again
     * every few seconds while the pop-up is still on screen.
     */
    private void startBell() {

        // entrance bounce: small -> a bit too big -> normal
        bellPane.setScaleX(0.3);
        bellPane.setScaleY(0.3);

        ScaleTransition grow = new ScaleTransition(Duration.millis(240), bellPane);
        grow.setToX(1.18);
        grow.setToY(1.18);
        grow.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition settle = new ScaleTransition(Duration.millis(170), bellPane);
        settle.setToX(1.0);
        settle.setToY(1.0);
        settle.setInterpolator(Interpolator.EASE_BOTH);

        new SequentialTransition(grow, settle).play();

        // ringing: (time in seconds, swing angle in degrees)
        double[][] swings = {
                {0.00, 0}, {0.07, -24}, {0.19, 21}, {0.31, -16}, {0.43, 12},
                {0.55, -8}, {0.67, 5}, {0.79, -2}, {0.90, 0}, {2.80, 0}};

        ringLoop = new Timeline();

        for (double[] point : swings) {
            ringLoop.getKeyFrames().add(new KeyFrame(
                    Duration.seconds(point[0]),
                    new KeyValue(swing.angleProperty(), point[1], Interpolator.EASE_BOTH)));
        }

        // inner waves flash first, outer waves a moment later
        addWavePulse(waveLeftInner, 0.00);
        addWavePulse(waveRightInner, 0.00);
        addWavePulse(waveLeftOuter, 0.09);
        addWavePulse(waveRightOuter, 0.09);

        ringLoop.setCycleCount(Timeline.INDEFINITE);
        ringLoop.play();
    }

    private void addWavePulse(SVGPath wave, double delay) {
        ringLoop.getKeyFrames().addAll(
                new KeyFrame(Duration.seconds(delay),
                        new KeyValue(wave.opacityProperty(), 0.0)),
                new KeyFrame(Duration.seconds(delay + 0.10),
                        new KeyValue(wave.opacityProperty(), 0.95, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.seconds(delay + 0.60),
                        new KeyValue(wave.opacityProperty(), 0.0, Interpolator.EASE_IN)));
    }

    @FXML
    private void handleClose() {
        dismiss();
    }

    private void dismiss() {
        if (closing) {
            return;
        }

        closing = true;

        if (countdown != null) {
            countdown.stop();
        }

        if (ringLoop != null) {
            ringLoop.stop();
        }

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), card);
        slide.setToX(SLIDE_DISTANCE);
        slide.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fade = new FadeTransition(Duration.millis(220), card);
        fade.setToValue(0);

        ParallelTransition leave = new ParallelTransition(slide, fade);
        leave.setOnFinished(event -> OverlayHost.remove(card));
        leave.play();
    }
}
