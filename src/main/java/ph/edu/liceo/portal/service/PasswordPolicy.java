package ph.edu.liceo.portal.service;

/**
 * The one password rule used by the forgot-password and change-password screens
 * (identical to the rule on the registration pages).
 */
public final class PasswordPolicy {

    public static final String REQUIREMENTS =
            "Password must be 8+ characters with uppercase, lowercase, number, and special character.";

    private PasswordPolicy() {
    }

    public static boolean isStrong(String password) {

        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for (char character : password.toCharArray()) {

            if (Character.isUpperCase(character)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(character)) {
                hasLowercase = true;
            } else if (Character.isDigit(character)) {
                hasNumber = true;
            } else {
                hasSpecial = true;
            }
        }

        return hasUppercase && hasLowercase && hasNumber && hasSpecial;
    }
}
