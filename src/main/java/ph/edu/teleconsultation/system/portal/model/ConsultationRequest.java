package ph.edu.teleconsultation.system.portal.model;

public class ConsultationRequest {

    private int id;
    private String patientName;
    private String doctorName;
    private String reason;
    private String date;
    private String status;
    private String notes = "";

    public ConsultationRequest(int id, String patientName, String doctorName,
                               String reason, String date) {
        this.id = id;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.reason = reason;
        this.date = date;
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

    public String getReason() {
        return reason;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }
}
