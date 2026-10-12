package ph.edu.liceo.portal.controller;

import java.time.LocalDate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.*;
import ph.edu.liceo.portal.service.AppointmentService;

public class AdminDashboardController extends DashboardShell {

    @FXML private Label adminNameLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label userCountLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label appointmentCountLabel;
    @FXML private Label requestUserLabel;
    @FXML private Label requestRoleLabel;
    @FXML private Label requestStatusLabel;

    @FXML
    private void initialize() {
        refreshNotificationBadge();

        User user = LoginController.getLoggedInUser();
        String name = user == null ? "Administrator" : user.getFullName();

        set(adminNameLabel, name);
        set(profileNameLabel, name);

        int pending = 0;
        User firstPending = null;

        for (User u : DataStore.users) {
            if (u instanceof Admin || u instanceof Patient) continue;
            if (u.getStatus() == AccountStatus.PENDING) {
                pending++;
                if (firstPending == null) firstPending = u;
            }
        }

        set(userCountLabel, String.valueOf(DataStore.users.size()));
        set(pendingCountLabel, String.valueOf(pending));
        set(appointmentCountLabel, String.valueOf(DataStore.appointments.size()));

        if (firstPending == null) {
            set(requestUserLabel, "No pending requests");
            set(requestRoleLabel, "All accounts are verified");
            set(requestStatusLabel, "NONE");
        } else {
            set(requestUserLabel, firstPending.getFullName());
            set(requestRoleLabel, AdminController.roleName(firstPending));
            set(requestStatusLabel, firstPending.getStatus().name());
        }
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        showPage("admin-dashboard.fxml");
    }

    @FXML
    private void handleDoctors(ActionEvent event) {
        showPage("admin-doctors.fxml");
    }

    @FXML
    private void handleVerificationRequests(ActionEvent event) {
        showPage("admin-verification-requests.fxml");
    }

    @FXML
    private void handleUsers(ActionEvent event) {
        showPage("admin-users.fxml");
    }

    @FXML
    private void handleAppointments(ActionEvent event) {
        showPage("admin-appointments.fxml");
    }

    @FXML
    private void handleMedicalRecords(ActionEvent event) {
        showPage("admin-medical-records.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        showPage("admin-profile.fxml");
    }
}
