package ph.edu.teleconsultation.system.portal.service;

import java.util.ArrayList;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.AccountStatus;
import ph.edu.teleconsultation.system.portal.model.Admin;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Notification;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.ConsultationRequest;
import ph.edu.teleconsultation.system.portal.model.MedicalRecord;
import ph.edu.teleconsultation.system.portal.model.Schedule;
import ph.edu.teleconsultation.system.portal.model.User;

public class UserService {

    public boolean register(User user) {

        if (user == null) {
            return false;
        }

        if (emailExists(user.getEmail()) || nameExists(user.getFullName())) {
            return false;
        }

        DataStore.users.add(user);

        NotificationService notifications = new NotificationService();

        if (user.getStatus() == AccountStatus.ACTIVE) {
            notifications.welcome(user);
        } else {
            String role = user.getClass().getSimpleName();
            String title = "Account awaiting verification";
            String message = user.getFullName() + " registered as " + role + ".";

            notifications.notifyRole(Admin.class, title, message,
                    Notification.Type.WARNING);

            if (!(user instanceof Secretary)) {
                notifications.notifyRole(Secretary.class, title, message,
                        Notification.Type.WARNING);
            }
        }

        return true;
    }

    public boolean emailExists(String email) {

        return DataStore.findUserByEmail(email) != null;
    }

    /**
     * Appointments, consultations and medical records refer to people by full
     * name, so two accounts must never share one (otherwise they would see
     * each other's records).
     */
    public boolean nameExists(String fullName) {

        if (fullName == null) {
            return false;
        }

        for (User user : DataStore.users) {

            if (user.getFullName().trim().equalsIgnoreCase(fullName.trim())) {
                return true;
            }
        }

        return false;
    }

    public boolean nameTakenByOther(User user, String fullName) {

        for (User other : DataStore.users) {

            if (other != user
                    && other.getFullName().trim().equalsIgnoreCase(fullName.trim())) {
                return true;
            }
        }

        return false;
    }

    public User findByEmail(String email) {

        return DataStore.findUserByEmail(email);
    }

    public User findById(int id) {

        for (User user : DataStore.users) {

            if (user.getId() == id) {
                return user;
            }
        }

        return null;
    }

    public ArrayList<User> getAllUsers() {
        return DataStore.users;
    }

    public ArrayList<User> getPendingUsers() {

        ArrayList<User> pendingUsers = new ArrayList<>();

        for (User user : DataStore.users) {

            if (user.getStatus() == AccountStatus.PENDING) {
                pendingUsers.add(user);
            }
        }

        return pendingUsers;
    }

    public void approveUser(User user) {

        if (user != null) {
            user.setStatus(AccountStatus.ACTIVE);

            new NotificationService().notifyUser(user, "Account approved",
                    "Your TeleConsult account has been verified. Welcome aboard!",
                    Notification.Type.SUCCESS);
        }
    }

    public void rejectUser(User user) {

        if (user != null) {
            user.setStatus(AccountStatus.REJECTED);

            // A doctor who can no longer sign in cannot see upcoming patients.
            if (user instanceof Doctor) {
                new AppointmentService().cancelUpcomingForDoctor(user.getFullName());
            }
        }
    }

    public void deleteUser(User user) {

        if (user != null) {
            if (user instanceof Doctor) {
                new AppointmentService().cancelUpcomingForDoctor(user.getFullName());
            }

            DataStore.removeUser(user);
            DataStore.notifications.remove(user);
        }
    }

    /**
     * Renames a user and keeps every record that stores the name as text
     * (appointments, consultation requests, medical records, schedules)
     * pointing at the renamed account.
     */
    public void renameUser(User user, String newName) {

        if (user == null || newName == null || newName.isBlank()) {
            return;
        }

        String oldName = user.getFullName();

        if (oldName.equals(newName)) {
            return;
        }

        user.setFullName(newName);

        for (Appointment a : DataStore.appointments) {
            if (a.getPatientName().equalsIgnoreCase(oldName)) a.setPatientName(newName);
            if (a.getDoctorName().equalsIgnoreCase(oldName)) a.setDoctorName(newName);
        }

        for (ConsultationRequest r : DataStore.consultationRequests) {
            if (r.getPatientName().equalsIgnoreCase(oldName)) r.setPatientName(newName);
            if (r.getDoctorName().equalsIgnoreCase(oldName)) r.setDoctorName(newName);
        }

        for (MedicalRecord m : DataStore.medicalRecords) {
            if (m.getPatientName().equalsIgnoreCase(oldName)) m.setPatientName(newName);
            if (m.getDoctorName().equalsIgnoreCase(oldName)) m.setDoctorName(newName);
        }

        Schedule schedule = DataStore.scheduleData.findByDoctor(oldName);
        if (schedule != null) {
            schedule.setDoctorName(newName);
        }
    }

    public boolean emailTakenByOther(User user, String email) {

        User found = DataStore.findUserByEmail(email);

        return found != null && found != user;
    }

    public ArrayList<User> getUsersWithStatus(AccountStatus status) {

        ArrayList<User> result = new ArrayList<>();

        for (User user : DataStore.users) {
            if (user.getStatus() == status) {
                result.add(user);
            }
        }

        return result;
    }
}
