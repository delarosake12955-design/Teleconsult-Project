package ph.edu.teleconsultation.system.portal.model;

public class Doctor extends User {

    private String licenseNumber;
    private String specialty;

    public Doctor(
            int id,
            String fullName,
            String email,
            String password,
            String licenseNumber,
            String specialty) {

        super(id, withTitle(fullName), email, password);
        this.licenseNumber = licenseNumber;
        this.specialty = specialty;
    }

    /**
     * Doctors are always shown as "Dr. Name". A registering doctor can type the plain
     * name (or already include "Dr." / "Dr") and it is stored in this one form, so
     * appointments, notifications and lists all use the same name.
     */
    public static String withTitle(String name) {
        if (name == null) {
            return "";
        }

        String clean = name.trim();
        String lower = clean.toLowerCase();

        if (lower.startsWith("dr.")) {
            clean = clean.substring(3).trim();
        } else if (lower.startsWith("dr ")) {
            clean = clean.substring(3).trim();
        }

        return clean.isEmpty() ? "" : "Dr. " + clean;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }
}