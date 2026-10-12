package ph.edu.liceo.portal.controller;

import java.util.Locale;

/**
 * Friendly checks for the vitals a patient types in at home.
 *
 * The ranges match {@code VitalSigns.check}, so anything that passes here is
 * accepted when the request is booked. There is no JavaFX in this class, which
 * keeps it small and easy to test.
 */
final class VitalsInput {

    enum State { EMPTY, OK, BAD }

    /** The result of checking one box: its state and a short message (may be null). */
    record Check(State state, String message) {

        static Check empty() {
            return new Check(State.EMPTY, null);
        }

        static Check ok(String note) {
            return new Check(State.OK, note);
        }

        static Check bad(String problem) {
            return new Check(State.BAD, problem);
        }

        boolean isBad() {
            return state == State.BAD;
        }
    }

    private VitalsInput() {
    }

    // ------------------------------------------------------------ temperature

    /** A reading between 90 and 110 is almost certainly Fahrenheit (98.6), so we convert it. */
    static boolean isFahrenheit(double value) {
        return value >= 90 && value <= 110;
    }

    static double toCelsius(double value) {
        return isFahrenheit(value) ? Math.round((value - 32) * 5 / 9 * 10) / 10.0 : value;
    }

    /** The temperature in degrees Celsius, or NaN if the text is not a number. */
    static double temperatureCelsius(String text) {
        Double value = number(text);
        return value == null ? Double.NaN : toCelsius(value);
    }

    static Check temperature(String text) {
        if (clean(text).isEmpty()) {
            return Check.empty();
        }

        Double value = number(text);

        if (value == null) {
            return Check.bad("Temperature: type a number, like 36.5.");
        }

        double celsius = toCelsius(value);

        if (celsius < 30 || celsius > 45) {
            return Check.bad("Temperature: that doesn't look right. Use \u00B0C, like 36.5.");
        }

        if (isFahrenheit(value)) {
            return Check.ok(String.format(Locale.ENGLISH,
                    "%.1f \u00B0F is %.1f \u00B0C. We'll save it in \u00B0C.", value, celsius));
        }

        return Check.ok(null);
    }

    // ------------------------------------------------------- whole-number boxes

    static Check pulse(String text) {
        return whole(text, 20, 250, "Pulse: between 20 and 250 beats per minute, like 72.");
    }

    static Check systolic(String text) {
        return whole(text, 50, 260, "Blood pressure: the top number should be between 50 and 260, like 120.");
    }

    /** The bottom number must be lower than the top one. */
    static Check diastolic(String text, String systolicText) {
        Check own = whole(text, 30, 160,
                "Blood pressure: the bottom number should be between 30 and 160, like 80.");

        if (own.state() != State.OK) {
            return own;
        }

        Check top = systolic(systolicText);

        if (top.state() == State.OK
                && Integer.parseInt(clean(systolicText)) <= Integer.parseInt(clean(text))) {
            return Check.bad("Blood pressure: the top number should be higher than the bottom number.");
        }

        return own;
    }

    static Check breathing(String text) {
        return whole(text, 4, 60, "Breathing: between 4 and 60 breaths per minute, like 16.");
    }

    // ----------------------------------------------------------------- helpers

    private static Check whole(String text, int min, int max, String problem) {
        String t = clean(text);

        if (t.isEmpty()) {
            return Check.empty();
        }

        try {
            int value = Integer.parseInt(t);
            return value < min || value > max ? Check.bad(problem) : Check.ok(null);
        } catch (NumberFormatException e) {
            return Check.bad(problem);
        }
    }

    private static Double number(String text) {
        try {
            return Double.parseDouble(clean(text).replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String clean(String text) {
        return text == null ? "" : text.trim();
    }
}
