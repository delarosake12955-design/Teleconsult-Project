package ph.edu.teleconsultation.system.portal.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A patient's rating of one completed appointment. There is at most one per appointment. */
public class Feedback {

    private final int appointmentId;
    private final String patientName;
    private final String doctorName;
    private final String appointmentDate;
    private int rating;
    private final ArrayList<String> tags = new ArrayList<>();
    private String comment = "";
    private final LocalDateTime createdAt;

    public Feedback(int appointmentId, String patientName, String doctorName, String appointmentDate,
                    int rating, List<String> tags, String comment) {
        this.appointmentId = appointmentId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.createdAt = LocalDateTime.now();
        update(rating, tags, comment);
    }

    /** The patient edits their feedback (only allowed inside the edit window, see FeedbackService). */
    public void update(int rating, List<String> newTags, String newComment) {
        this.rating = rating;
        this.tags.clear();
        if (newTags != null) {
            this.tags.addAll(newTags);
        }
        this.comment = newComment == null ? "" : newComment.trim();
    }

    public int getAppointmentId() {
        return appointmentId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public int getRating() {
        return rating;
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** "★★★★☆" style text for tables and cards. */
    public String stars() {
        return stars(rating);
    }

    public static String stars(int value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= value ? '\u2605' : '\u2606');
        }
        return sb.toString();
    }
}
