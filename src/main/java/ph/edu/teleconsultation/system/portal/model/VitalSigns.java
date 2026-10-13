package ph.edu.teleconsultation.system.portal.model;

public class VitalSigns {

    private double temperature;
    private int heartRate;
    private int systolic;
    private int diastolic;
    private int respiratoryRate;
    private String recordedBy;
    private String recordedAt;

    public VitalSigns(double temperature, int heartRate, int systolic,
                      int diastolic, int respiratoryRate) {
        this.temperature = temperature;
        this.heartRate = heartRate;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.respiratoryRate = respiratoryRate;
    }

    /** Realistic-range check shared by every form. Returns an error message, or null when valid. */
    public static String check(double temperature, int heartRate, int systolic,
                               int diastolic, int respiratoryRate) {
        if (temperature < 30 || temperature > 45) return "Temperature must be between 30 and 45 \u00B0C.";
        if (heartRate < 20 || heartRate > 250) return "Heart rate must be between 20 and 250 bpm.";
        if (systolic < 50 || systolic > 260 || diastolic < 30 || diastolic > 160 || systolic <= diastolic) {
            return "Enter a realistic blood pressure (systolic above diastolic).";
        }
        if (respiratoryRate < 4 || respiratoryRate > 60) return "Breathing rate must be between 4 and 60 per minute.";
        return null;
    }

    /** One-line text for tables, e.g. "36.8 C | 72 bpm | 120/80". */
    public String summary() {
        return String.format("%.1f C | %d bpm | %d/%d", temperature, heartRate, systolic, diastolic);
    }

    public double getTemperature() {
        return temperature;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public int getSystolic() {
        return systolic;
    }

    public int getDiastolic() {
        return diastolic;
    }

    public int getRespiratoryRate() {
        return respiratoryRate;
    }

    /** Remembers which staff member recorded these vitals, and when. */
    public void stamp(String staffName) {
        this.recordedBy = staffName;
        this.recordedAt = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", java.util.Locale.ENGLISH));
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public String getRecordedAt() {
        return recordedAt;
    }
}
