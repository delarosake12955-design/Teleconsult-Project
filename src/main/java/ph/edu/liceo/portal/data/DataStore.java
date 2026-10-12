package ph.edu.liceo.portal.data;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

import ph.edu.liceo.portal.model.Appointment;
import ph.edu.liceo.portal.model.ConsultationRequest;
import ph.edu.liceo.portal.model.MedicalRecord;
import ph.edu.liceo.portal.model.Notification;
import ph.edu.liceo.portal.model.User;

public class DataStore {

    public static ArrayList<User> users = new ArrayList<>();
    public static ArrayList<Appointment> appointments = new ArrayList<>();
    public static ArrayList<ConsultationRequest> consultationRequests = new ArrayList<>();
    public static ArrayList<MedicalRecord> medicalRecords = new ArrayList<>();
    public static ScheduleData scheduleData = new ScheduleData();

    /**
     * Index of users by lower-cased e-mail so login and "does this e-mail
     * exist" are O(1) HashMap lookups instead of scanning the whole list.
     * The list stays the source of truth; the index repairs itself whenever
     * it misses or finds a stale entry (e-mail edited, user added directly).
     */
    private static final HashMap<String, User> usersByEmail = new HashMap<>();

    public static User findUserByEmail(String email) {

        if (email == null) {
            return null;
        }

        String key = email.trim().toLowerCase();
        User user = usersByEmail.get(key);

        if (user != null && user.getEmail().trim().equalsIgnoreCase(key)) {
            return user;
        }

        // Miss or stale entry: rebuild the index once and look again.
        usersByEmail.clear();

        for (User u : users) {
            usersByEmail.put(u.getEmail().trim().toLowerCase(), u);
        }

        return usersByEmail.get(key);
    }

    public static void removeUser(User user) {

        if (users.remove(user) && user != null) {
            usersByEmail.remove(user.getEmail().trim().toLowerCase());
        }
    }

    /**
     * Each user's notifications live in their own stack (an ArrayDeque used
     * with push), so the newest one is always on top. The HashMap finds a
     * user's stack in O(1).
     */
    public static final HashMap<User, ArrayDeque<Notification>> notifications = new HashMap<>();

    private static int nextNotificationId = 1;

    public static int getNextNotificationId() {
        return nextNotificationId++;
    }

    private static int nextUserId = 1;
    private static int nextAppointmentId = 1;
    private static int nextConsultationId = 1;
    private static int nextMedicalRecordId = 1;

    public static int getNextUserId() {
        return nextUserId++;
    }

    public static int getNextAppointmentId() {
        return nextAppointmentId++;
    }

    public static int getNextConsultationId() {
        return nextConsultationId++;
    }

    public static int getNextMedicalRecordId() {
        return nextMedicalRecordId++;
    }
}