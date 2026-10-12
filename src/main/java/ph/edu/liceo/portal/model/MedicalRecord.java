package ph.edu.liceo.portal.model;

public class MedicalRecord {

    private int id;
    private String patientName;
    private String doctorName;
    private String date;
    private String diagnosis;
    private String treatment;
    private VitalSigns vitalSigns;

    public MedicalRecord(int id, String patientName, String doctorName,
                         String date, String diagnosis, String treatment,
                         VitalSigns vitalSigns) {
        this.id = id;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.vitalSigns = vitalSigns;
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

    public String getDate() {
        return date;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public VitalSigns getVitalSigns() {
        return vitalSigns;
    }

    public void setVitalSigns(VitalSigns vitalSigns) {
        this.vitalSigns = vitalSigns;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }
}
