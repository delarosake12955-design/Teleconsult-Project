package ph.edu.teleconsultation.system.portal.model;

public class Nurse extends User {

    private String licenseNumber;
    private String department;

    public Nurse(
            int id,
            String fullName,
            String email,
            String password,
            String licenseNumber,
            String department) {

        super(id, fullName, email, password);
        this.licenseNumber = licenseNumber;
        this.department = department;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}