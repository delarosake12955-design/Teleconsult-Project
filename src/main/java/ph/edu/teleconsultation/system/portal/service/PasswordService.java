package ph.edu.teleconsultation.system.portal.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Notification;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.User;

/**
 * Change password (signed in) and forgot password (signed out).
 *
 * There is no e-mail or database in this system, so a forgotten password is reset by
 * proving who you are with details given at registration: the e-mail, the full name and
 * one role detail (patient: date of birth, doctor / nurse: license number,
 * secretary: department). Wrong guesses are limited, like sign-in attempts are.
 */
public class PasswordService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCK_MILLIS = 60_000;

    private static final Map<String, Integer> verifyFailures = new HashMap<>();
    private static final Map<String, Long> verifyLockedUntil = new HashMap<>();

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final NotificationService notificationService = new NotificationService();

    /** Outcome of a password action: success flag and a message the screen can show. */
    public static final class Result {

        private final boolean ok;
        private final String message;

        private Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }

        static Result ok(String message) {
            return new Result(true, message);
        }

        static Result fail(String message) {
            return new Result(false, message);
        }
    }

    // ------------------------------------------------------------ change password

    /** A signed-in user changes their own password; the current one must be given. */
    public Result changePassword(User user, String current, String newPassword, String confirm) {

        if (user == null) {
            return Result.fail("You need to be signed in to change your password.");
        }

        if (current == null || current.isEmpty()) {
            return Result.fail("Enter your current password.");
        }

        if (!user.checkPassword(current)) {
            return Result.fail("Your current password is incorrect.");
        }

        String problem = checkNewPassword(newPassword, confirm);

        if (problem != null) {
            return Result.fail(problem);
        }

        if (user.checkPassword(newPassword)) {
            return Result.fail("Your new password must be different from your current one.");
        }

        user.setPassword(newPassword);

        notificationService.notifyUser(user,
                "Password changed",
                "Your password was changed just now. If this was not you, contact the administrator.",
                Notification.Type.SUCCESS);

        return Result.ok("Your password was changed.");
    }

    // ------------------------------------------------------------ forgot password

    /**
     * @param roleName "Patient", "Doctor", "Nurse" or "Secretary" (as picked on the screen)
     * @param detail   patient: date of birth; doctor / nurse: license number; secretary: department
     */
    public Result resetPassword(String roleName, String email, String fullName, String detail,
                                String newPassword, String confirm) {
        return resetPassword(roleName, email, fullName, detail, null, newPassword, confirm);
    }

    /**
     * Same as above, but patients must also give the Patient ID generated when they
     * registered (patientId is ignored for the other roles).
     */
    public Result resetPassword(String roleName, String email, String fullName, String detail,
                                String patientId, String newPassword, String confirm) {

        boolean patient = "Patient".equals(roleName);

        if (isBlank(email) || isBlank(fullName) || isBlank(detail)) {
            return Result.fail("Please fill in all the details.");
        }

        if (patient && isBlank(patientId)) {
            return Result.fail("Enter your Patient ID.");
        }

        if (isVerificationLocked(email)) {
            return Result.fail("Too many attempts. Please wait a minute and try again.");
        }

        // Typos in the new password never count as a failed identity check.
        String problem = checkNewPassword(newPassword, confirm);

        if (problem != null) {
            return Result.fail(problem);
        }

        User user = DataStore.findUserByEmail(email);

        // Same answer for an unknown e-mail and for wrong details, so accounts cannot be probed.
        if (user == null || !detailsMatch(user, roleName, fullName, detail)) {
            recordVerificationFailure(email);
            return Result.fail("We could not verify those details. Please check them and try again.");
        }

        // Patients must also give their Patient ID; same answer as any other wrong detail.
        if (patient && !(user instanceof Patient p && same(p.getPatientId(), patientId))) {
            recordVerificationFailure(email);
            return Result.fail("We could not verify those details. Please check them and try again.");
        }

        user.setPassword(newPassword);

        verifyFailures.remove(key(email));
        verifyLockedUntil.remove(key(email));
        authenticationService.clearFailures(email);   // sign in again right away

        notificationService.notifyUser(user,
                "Password reset",
                "Your password was reset from the sign-in page. If this was not you, contact the administrator.",
                Notification.Type.WARNING);

        return Result.ok("Your password was updated. You can sign in now.");
    }

    public boolean isVerificationLocked(String email) {

        String key = key(email);
        Long until = verifyLockedUntil.get(key);

        if (until == null) {
            return false;
        }

        if (System.currentTimeMillis() >= until) {
            verifyLockedUntil.remove(key);
            verifyFailures.remove(key);
            return false;
        }

        return true;
    }

    // ------------------------------------------------------------------- helpers

    private String checkNewPassword(String newPassword, String confirm) {

        if (newPassword == null || newPassword.isEmpty()) {
            return "Enter a new password.";
        }

        if (!PasswordPolicy.isStrong(newPassword)) {
            return PasswordPolicy.REQUIREMENTS;
        }

        if (!newPassword.equals(confirm)) {
            return "The two new passwords do not match.";
        }

        return null;
    }

    private boolean detailsMatch(User user, String roleName, String fullName, String detail) {

        if (!same(user.getFullName(), fullName)) {
            return false;
        }

        if (roleName == null) {
            return false;
        }

        return switch (roleName) {
            case "Patient" -> user instanceof Patient patient
                    && sameDate(patient.getDateOfBirth(), detail);
            case "Doctor" -> user instanceof Doctor doctor
                    && same(doctor.getLicenseNumber(), detail);
            case "Nurse" -> user instanceof Nurse nurse
                    && same(nurse.getLicenseNumber(), detail);
            case "Secretary" -> user instanceof Secretary secretary
                    && same(secretary.getDepartment(), detail);
            default -> false;
        };
    }

    private void recordVerificationFailure(String email) {

        String key = key(email);
        int count = verifyFailures.merge(key, 1, Integer::sum);

        if (count >= MAX_ATTEMPTS) {
            verifyLockedUntil.put(key, System.currentTimeMillis() + LOCK_MILLIS);
        }
    }

    private boolean same(String stored, String typed) {
        return stored != null && typed != null
                && stored.trim().equalsIgnoreCase(typed.trim());
    }

    /** Dates are accepted as 2004-01-15, 01/15/2004 or 1/15/2004. */
    private boolean sameDate(String stored, String typed) {

        LocalDate a = parseDate(stored);
        LocalDate b = parseDate(typed);

        if (a != null && b != null) {
            return a.equals(b);
        }

        return same(stored, typed);
    }

    private LocalDate parseDate(String text) {

        if (text == null) {
            return null;
        }

        String value = text.trim();

        for (String pattern : new String[]{"yyyy-MM-dd", "MM/dd/yyyy", "M/d/yyyy"}) {

            try {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }

        return null;
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
