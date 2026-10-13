package ph.edu.teleconsultation.system.portal.service;

/**
 * Basic shape check for doctor and nurse license numbers.
 *
 * Real license numbers differ (for example a plain 7-digit number, or the school's own
 * "MD-2026-001" style used in the demo), so only the shape is checked: 4 to 20 characters,
 * letters, numbers and dashes, no spaces, and at least one digit. This stops entries such
 * as "abc" or "n/a" without rejecting a genuine number.
 */
public final class LicenseRules {

    private LicenseRules() {
    }

    public static boolean isValid(String license) {
        return license != null
                && license.matches("(?=.*\\d)[A-Za-z0-9][A-Za-z0-9-]{2,18}[A-Za-z0-9]");
    }

    public static String message(String example) {
        return "Enter a valid license number, for example " + example
                + " (letters, numbers and dashes, 4 to 20 characters, no spaces).";
    }
}
