package ph.edu.liceo.portal.controller;

import java.time.LocalDate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.*;
import ph.edu.liceo.portal.service.AppointmentService;
import ph.edu.liceo.portal.service.MedicalRecordService;

public class NurseDashboardController extends DashboardShell {

    @FXML private Label nurseNameLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label departmentLabel;
    @FXML private Label recentPatientLabel;
    @FXML private Label recentActivityLabel;
    @FXML private Label recentActivityStatusLabel;

    @FXML
    private void initialize() {
        refreshNotificationBadge();

        User user = LoginController.getLoggedInUser();

        set(nurseNameLabel, user == null ? "Nurse" : user.getFullName());
        set(profileNameLabel, user == null ? "Nurse" : user.getFullName());
        set(departmentLabel, user instanceof Nurse nurse && nurse.getDepartment() != null
                ? nurse.getDepartment() : "Clinical Services");

        // First priority: new requests waiting for nurse review (FIFO, oldest first).
        java.util.Queue<Appointment> toReview = new AppointmentService().getNurseReviewQueue();

        if (!toReview.isEmpty()) {
            Appointment nextRequest = toReview.peek();

            set(recentPatientLabel, "Next to review: " + nextRequest.getPatientName()
                    + (nextRequest.isPriority() ? "  [" + nextRequest.getPatientType().getLabel() + "]" : ""));
            set(recentActivityLabel, toReview.size() + " request(s) waiting for your review \u2022 "
                    + nextRequest.getDoctorName() + " on " + nextRequest.getDate());
            set(recentActivityStatusLabel, "NEEDS REVIEW");

            return;
        }

        // Then records that were written without vitals: a FIFO queue, oldest first.
        java.util.Queue<MedicalRecord> waiting = new MedicalRecordService().getVitalsQueue();

        if (!waiting.isEmpty()) {
            MedicalRecord next = waiting.peek();

            set(recentPatientLabel, "Next: " + next.getPatientName());
            set(recentActivityLabel, waiting.size() + " waiting for vitals \u2022 "
                    + next.getDiagnosis() + " \u2022 " + next.getDate());
            set(recentActivityStatusLabel, "NEEDS VITALS");

            return;
        }

        MedicalRecord latest = null;

        for (MedicalRecord record : DataStore.medicalRecords) {
            if (latest == null || record.getDate().compareTo(latest.getDate()) >= 0) {
                latest = record;
            }
        }

        if (latest == null) {
            set(recentPatientLabel, "No recent activity");
            set(recentActivityLabel, "No medical records have been added yet");
            set(recentActivityStatusLabel, "NONE");
        } else {
            set(recentPatientLabel, latest.getPatientName());
            set(recentActivityLabel, latest.getDiagnosis() + " • " + latest.getDate()
                    + (latest.getVitalSigns() == null ? " • vitals not recorded" : ""));
            set(recentActivityStatusLabel,
                    latest.getVitalSigns() == null ? "NEEDS VITALS" : "RECORDED");
        }
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        showPage("nurse-dashboard.fxml");
    }

    @FXML
    private void handleAppointments(ActionEvent event) {
        showPage("nurse-appointments.fxml");
    }

    @FXML
    private void handlePatients(ActionEvent event) {
        showPage("nurse-patients.fxml");
    }

    @FXML
    private void handleVitalSigns(ActionEvent event) {
        showPage("nurse-medical-records.fxml");
    }

    @FXML
    private void handleConsultations(ActionEvent event) {
        showPage("nurse-consultation.fxml");
    }

    @FXML
    private void handleMedicalRecords(ActionEvent event) {
        showPage("nurse-medical-records.fxml");
    }

    @FXML
    private void handleConsultation(ActionEvent event) {
        showPage("nurse-consultation.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        showPage("nurse-profile.fxml");
    }
}
