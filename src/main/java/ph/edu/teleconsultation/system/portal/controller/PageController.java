package ph.edu.teleconsultation.system.portal.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;

/**
 * Base class for every page controller. It provides the sidebar navigation
 * handlers used across all role pages, so each page works identically whether
 * it is embedded in a dashboard shell or opened on its own.
 */
public abstract class PageController {

    @FXML
    protected void handleDashboard(ActionEvent event) {
        go(event, "dashboard");
    }

    @FXML
    protected void handleAppointments(ActionEvent event) {
        go(event, "appointments");
    }

    @FXML
    protected void handleDoctors(ActionEvent event) {
        go(event, "doctors");
    }

    @FXML
    protected void handlePatients(ActionEvent event) {
        go(event, "patients");
    }

    @FXML
    protected void handleRequests(ActionEvent event) {
        go(event, "requests");
    }

    @FXML
    protected void handleConsultation(ActionEvent event) {
        go(event, "consultation");
    }

    @FXML
    protected void handleConsultations(ActionEvent event) {
        go(event, "consultation");
    }

    @FXML
    protected void handleMedicalRecords(ActionEvent event) {
        go(event, "records");
    }

    @FXML
    protected void handleVitalSigns(ActionEvent event) {
        go(event, "records");
    }

    @FXML
    protected void handleProfile(ActionEvent event) {
        go(event, "profile");
    }

    @FXML
    protected void handleVerification(ActionEvent event) {
        go(event, "verification");
    }

    @FXML
    protected void handleVerificationRequests(ActionEvent event) {
        go(event, "verification");
    }

    @FXML
    protected void handleUsers(ActionEvent event) {
        go(event, "users");
    }

    @FXML
    protected void handleSchedule(ActionEvent event) {
        go(event, "schedule");
    }

    @FXML
    protected void handleLogout(ActionEvent event) {
        Navigator.logout(source(event));
    }

    protected void go(ActionEvent event, String section) {
        Navigator.openSection(source(event), section);
    }

    protected Node source(ActionEvent event) {
        return event == null ? null : (Node) event.getSource();
    }
}
