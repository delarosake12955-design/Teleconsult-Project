package ph.edu.liceo.portal.model;

public class Appointment {

    private int id;
    private String patientName;
    private String doctorName;
    private String specialty;
    private String date;
    private String time;
    private String category;
    private String notes;
    private String status;
    /** Reported by the patient with the request (home device); null if none was given. */
    private VitalSigns vitals;
    /** True once a nurse has checked or entered the vitals herself. */
    private boolean vitalsChecked;
    /** Senior / PWD / Regular, saved when the request is made so later profile edits never rewrite it. */
    private PatientType patientType = PatientType.REGULAR;
    /** The patient's PWD ID number (empty unless the patient is a PWD), for the nurse to check. */
    private String pwdIdNumber = "";
    /**
     * True once a nurse has forwarded this request to the doctor. Until then the
     * doctor must not see it (SRS workflow: nurse review comes first).
     */
    private boolean forwardedToDoctor;

    public Appointment(int id, String patientName, String doctorName,
                       String specialty, String date, String time,
                       String category, String notes) {
        this.id = id;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.date = date;
        this.time = time;
        this.category = category;
        this.notes = notes;
        this.status = "PENDING";
    }

    public int getId() {
        return id;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getSpecialty() {
        return specialty;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getCategory() {
        return category;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public VitalSigns getVitals() {
        return vitals;
    }

    public void setVitals(VitalSigns vitals) {
        this.vitals = vitals;
    }

    public boolean isVitalsChecked() {
        return vitalsChecked;
    }

    public void setVitalsChecked(boolean vitalsChecked) {
        this.vitalsChecked = vitalsChecked;
    }

    public void setStatus(String status) {
        this.status = status;

        // Any status past the nurse's review means the doctor has been given the request.
        if ("FOR_DOCTOR_REVIEW".equalsIgnoreCase(status)
                || "DOCTOR_APPROVED".equalsIgnoreCase(status)
                || "CONFIRMED".equalsIgnoreCase(status)
                || "COMPLETED".equalsIgnoreCase(status)) {
            this.forwardedToDoctor = true;
        }
    }

    /** False while the nurse is still reviewing, or if the nurse returned it / the patient cancelled it first. */
    public boolean isForwardedToDoctor() {
        return forwardedToDoctor;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public PatientType getPatientType() {
        return patientType;
    }

    public void setPatientType(PatientType patientType) {
        this.patientType = patientType == null ? PatientType.REGULAR : patientType;
    }

    public String getPwdIdNumber() {
        return pwdIdNumber;
    }

    public void setPwdIdNumber(String pwdIdNumber) {
        this.pwdIdNumber = pwdIdNumber == null ? "" : pwdIdNumber.trim();
    }

    /** Seniors and PWDs are reviewed before regular patients. */
    public boolean isPriority() {
        return patientType.isPriority();
    }
}