package ph.edu.teleconsultation.system.portal.model;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

public class Patient extends User {

    private String dateOfBirth;
    private String phoneNumber;
    private String address;
    /** Declared by the patient; a PWD ID number is kept with it so staff can check it. */
    private boolean pwd;
    private String pwdIdNumber = "";

    public Patient(
            int id,
            String fullName,
            String email,
            String password,
            String dateOfBirth,
            String phoneNumber,
            String address) {

        super(id, fullName, email, password);
        this.dateOfBirth = dateOfBirth;
        this.phoneNumber = phoneNumber;
        this.address = address;
        setStatus(AccountStatus.ACTIVE);
    }

    /** Generated automatically at registration from the account number, e.g. PT-00006. */
    public String getPatientId() {
        return String.format("PT-%05d", getId());
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // ------------------------------------------------------------ patient type

    public boolean isPwd() {
        return pwd;
    }

    public String getPwdIdNumber() {
        return pwdIdNumber;
    }

    public void setPwd(boolean pwd) {
        this.pwd = pwd;
        if (!pwd) {
            this.pwdIdNumber = "";
        }
    }

    public void setPwdIdNumber(String pwdIdNumber) {
        this.pwdIdNumber = pwdIdNumber == null ? "" : pwdIdNumber.trim();
    }

    /** Age in whole years on the given day, or -1 when the date of birth is missing or unreadable. */
    public int getAge(LocalDate on) {
        if (dateOfBirth == null || dateOfBirth.isBlank() || on == null) {
            return -1;
        }

        try {
            LocalDate born = LocalDate.parse(dateOfBirth.trim());
            return born.isAfter(on) ? -1 : Period.between(born, on).getYears();
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    public int getAge() {
        return getAge(LocalDate.now());
    }

    public boolean isSenior() {
        return getAge() >= PatientType.SENIOR_AGE;
    }

    /** Senior is calculated from the date of birth, PWD is declared, everyone else is regular. */
    public PatientType getPatientType() {
        return PatientType.of(isSenior(), pwd);
    }
}