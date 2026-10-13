package ph.edu.teleconsultation.system.portal.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Admin;
import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.Feedback;
import ph.edu.teleconsultation.system.portal.model.Notification;

/**
 * Patient feedback after a visit. A patient is "done" when the doctor marks the appointment
 * COMPLETED, so that is the only time feedback can be given. One feedback per appointment,
 * editable for {@value #EDIT_WINDOW_HOURS} hours, then locked.
 */
public class FeedbackService {

    public static final int EDIT_WINDOW_HOURS = 24;
    public static final int MAX_COMMENT_LENGTH = 300;
    /** Ratings at or below this raise an alert for the admin to follow up. */
    public static final int LOW_RATING = 2;

    private final NotificationService notifications = new NotificationService();

    public Feedback getFor(Appointment appointment) {
        if (appointment == null) return null;
        for (Feedback f : DataStore.feedbacks) {
            if (f.getAppointmentId() == appointment.getId()) {
                return f;
            }
        }
        return null;
    }

    private boolean stillEditable(Feedback feedback) {
        return Duration.between(feedback.getCreatedAt(), LocalDateTime.now()).toHours() < EDIT_WINDOW_HOURS;
    }

    /** Null when the patient may give (or change) feedback for this appointment, otherwise why not. */
    public String problem(Appointment appointment, String patientName) {
        if (appointment == null) {
            return "Select a completed appointment first.";
        }
        if (patientName == null || appointment.getPatientName() == null
                || !appointment.getPatientName().equalsIgnoreCase(patientName)) {
            return "You can only give feedback on your own appointments.";
        }
        if (!AppointmentService.COMPLETED.equalsIgnoreCase(appointment.getStatus())) {
            return "Feedback opens once the doctor marks the appointment as completed.";
        }
        Feedback existing = getFor(appointment);
        if (existing != null && !stillEditable(existing)) {
            return "Your feedback was sent and can no longer be edited (it can be changed for "
                    + EDIT_WINDOW_HOURS + " hours).";
        }
        return null;
    }

    /** Saves new feedback or updates the earlier one. Returns false if it is not allowed or the rating is invalid. */
    public boolean submit(Appointment appointment, String patientName, int rating,
                          List<String> tags, String comment) {
        if (problem(appointment, patientName) != null || rating < 1 || rating > 5) {
            return false;
        }

        String text = comment == null ? "" : comment.trim();
        if (text.length() > MAX_COMMENT_LENGTH) {
            text = text.substring(0, MAX_COMMENT_LENGTH);
        }

        Feedback existing = getFor(appointment);
        if (existing != null) {
            existing.update(rating, tags, text);
        } else {
            DataStore.feedbacks.add(new Feedback(appointment.getId(), appointment.getPatientName(),
                    appointment.getDoctorName(), appointment.getDate(), rating, tags, text));
        }

        notifications.notifyByName(appointment.getDoctorName(),
                existing == null ? "New patient feedback" : "Patient feedback updated",
                appointment.getPatientName() + " rated the visit on " + appointment.getDate() + " "
                        + Feedback.stars(rating) + (text.isEmpty() ? "" : " \u2014 " + text),
                rating <= LOW_RATING ? Notification.Type.WARNING : Notification.Type.INFO);

        if (rating <= LOW_RATING) {
            notifications.notifyRole(Admin.class, "Low rating to follow up",
                    appointment.getPatientName() + " gave " + appointment.getDoctorName() + " "
                            + Feedback.stars(rating) + " for the visit on " + appointment.getDate() + ".",
                    Notification.Type.ALERT);
        }
        return true;
    }

    /** Completed visits of this patient that have no feedback yet. */
    public int waitingCount(String patientName, List<Appointment> appointments) {
        int count = 0;
        for (Appointment a : appointments) {
            if (patientName != null && patientName.equalsIgnoreCase(a.getPatientName())
                    && AppointmentService.COMPLETED.equalsIgnoreCase(a.getStatus())
                    && getFor(a) == null) {
                count++;
            }
        }
        return count;
    }

    /** One doctor's feedback, newest first. */
    public ArrayList<Feedback> getForDoctor(String doctorName) {
        ArrayList<Feedback> result = new ArrayList<>();
        for (Feedback f : DataStore.feedbacks) {
            if (f.getDoctorName() != null && f.getDoctorName().equalsIgnoreCase(doctorName)) {
                result.add(f);
            }
        }
        result.sort((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt()));
        return result;
    }

    /** Everything, newest first (admin). */
    public ArrayList<Feedback> getAll() {
        ArrayList<Feedback> result = new ArrayList<>(DataStore.feedbacks);
        result.sort((x, y) -> y.getCreatedAt().compareTo(x.getCreatedAt()));
        return result;
    }

    public static double average(List<Feedback> items) {
        if (items.isEmpty()) return 0;
        double sum = 0;
        for (Feedback f : items) sum += f.getRating();
        return sum / items.size();
    }
}
