package ph.edu.teleconsultation.system.portal.controller;

import java.time.LocalDate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.*;
import ph.edu.teleconsultation.system.portal.service.AppointmentService;

public class SecretaryDashboardController extends DashboardShell {

    @FXML private Label secretaryNameLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label departmentLabel;
    @FXML private Label appointmentCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label verificationCountLabel;
    @FXML private Label verificationSummaryLabel;
    @FXML private Label appointmentPatientLabel;
    @FXML private Label appointmentDoctorLabel;
    @FXML private Label appointmentStatusLabel;

    @FXML
    private void initialize() {
        refreshNotificationBadge();

        User user = LoginController.getLoggedInUser();

        set(secretaryNameLabel, user == null ? "Secretary" : user.getFullName());
        set(profileNameLabel, user == null ? "Secretary" : user.getFullName());
        set(departmentLabel, user instanceof Secretary s && s.getDepartment() != null
                ? s.getDepartment() : "Medical Administration");

        // Requests the doctor has approved are waiting for the secretary to schedule.
        int awaiting = 0;
        Appointment first = null;

        for (Appointment a : DataStore.appointments) {
            if (AppointmentService.DOCTOR_APPROVED.equalsIgnoreCase(a.getStatus())) {
                awaiting++;
                if (first == null) first = a;
            }
        }

        int pendingAccounts = 0;

        for (User u : DataStore.users) {
            if (!(u instanceof Admin) && !(u instanceof Patient)
                    && u.getStatus() == AccountStatus.PENDING) {
                pendingAccounts++;
            }
        }

        set(appointmentCountLabel, String.valueOf(DataStore.appointments.size()));
        set(pendingCountLabel, String.valueOf(awaiting));
        set(verificationCountLabel, String.valueOf(pendingAccounts));
        set(verificationSummaryLabel, pendingAccounts == 0
                ? "No accounts are waiting for verification"
                : pendingAccounts + " account(s) waiting for verification");

        if (first == null) {
            set(appointmentPatientLabel, "No requests to schedule");
            set(appointmentDoctorLabel, "Doctor-approved requests will appear here");
            set(appointmentStatusLabel, "NONE");
        } else {
            set(appointmentPatientLabel, first.getPatientName());
            set(appointmentDoctorLabel, first.getDoctorName() + " • " + first.getDate()
                    + " • " + first.getTime());
            set(appointmentStatusLabel, first.getStatus().replace('_', ' '));
        }
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        showPage("secretary-dashboard.fxml");
    }

    @FXML
    private void handleAppointments(ActionEvent event) {
        showPage("secretary-appointments.fxml");
    }

    @FXML
    private void handleVerificationRequests(ActionEvent event) {
        showPage("secretary-verification-requests.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        showPage("secretary-profile.fxml");
    }

    @FXML
    private void handleDoctors(ActionEvent event) {
        showPage("secretary-doctors.fxml");
    }

    @FXML
    private void handlePatients(ActionEvent event) {
        showPage("secretary-patients.fxml");
    }
}
