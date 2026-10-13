package ph.edu.teleconsultation.system.portal.service;

import java.util.HashMap;
import java.util.Map;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.AccountStatus;
import ph.edu.teleconsultation.system.portal.model.User;

public class AuthenticationService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCK_MILLIS = 60_000;

    private static final Map<String, Integer> failures = new HashMap<>();
    private static final Map<String, Long> lockedUntil = new HashMap<>();

    /** True while this email is temporarily locked after repeated failed sign-ins. */
    public boolean isLockedOut(String email) {
        String key = key(email);
        Long until = lockedUntil.get(key);

        if (until == null) {
            return false;
        }

        if (System.currentTimeMillis() >= until) {
            lockedUntil.remove(key);
            failures.remove(key);
            return false;
        }

        return true;
    }

    /** Forgets earlier failed sign-ins (used after a successful password reset). */
    public void clearFailures(String email) {
        failures.remove(key(email));
        lockedUntil.remove(key(email));
    }

    public User login(String email, String password) {

        if (email == null || password == null || isLockedOut(email)) {
            return null;
        }

        User user = DataStore.findUserByEmail(email);

        if (user != null && user.checkPassword(password)) {

            if (user.getStatus() == AccountStatus.ACTIVE) {
                failures.remove(key(email));
                return user;
            }

            return null;
        }

        recordFailure(email);

        return null;
    }

    private void recordFailure(String email) {
        String key = key(email);
        int count = failures.merge(key, 1, Integer::sum);

        if (count >= MAX_ATTEMPTS) {
            lockedUntil.put(key, System.currentTimeMillis() + LOCK_MILLIS);
        }
    }

    private String key(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
