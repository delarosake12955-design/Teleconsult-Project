package ph.edu.liceo.portal.model;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class User {

    private static final int ITERATIONS = 65536;
    private static final SecureRandom RANDOM = new SecureRandom();

    private int id;
    private String fullName;
    private String email;
    private String passwordSalt;
    private String passwordHash;
    private AccountStatus status;

    // Profile customization (kept in memory like the rest of the data).
    private byte[] photo;
    private String themeKey = ProfileTheme.DEFAULT_KEY;
    private String bio = "";

    public User(int id, String fullName, String email, String password) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        setPassword(password);
        this.status = AccountStatus.PENDING;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    /** Passwords are never stored in plain text; only a salted PBKDF2 hash is kept. */
    public boolean checkPassword(String password) {
        if (password == null || passwordHash == null) {
            return false;
        }

        byte[] expected = Base64.getDecoder().decode(passwordHash);
        byte[] actual = hash(password, Base64.getDecoder().decode(passwordSalt));

        return MessageDigest.isEqual(expected, actual);
    }

    /** Profile photo bytes (PNG/JPG/GIF), or null for the initials avatar. */
    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getThemeKey() {
        return themeKey;
    }

    public void setThemeKey(String themeKey) {
        this.themeKey = ProfileTheme.byKey(themeKey).key();
    }

    /** Short "about me" line shown under the name; never null. */
    public String getBio() {
        return bio == null ? "" : bio;
    }

    public void setBio(String bio) {
        this.bio = bio == null ? "" : bio.trim();
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        this.passwordSalt = Base64.getEncoder().encodeToString(salt);
        this.passwordHash = Base64.getEncoder().encodeToString(
                hash(password == null ? "" : password, salt));
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    private static byte[] hash(String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        }
    }
}
