package ph.edu.liceo.portal.controller;

import java.time.LocalDate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.*;
import ph.edu.liceo.portal.service.AppointmentService;

public class PatientDashboardController extends DashboardShell {

    @FXML private Label patientNameLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label appointmentStatusLabel;
    @FXML private Label upcomingDoctorLabel;
    @FXML private Label upcomingSpecialtyLabel;
    @FXML private Label upcomingDateLabel;
    @FXML private Label upcomingTimeLabel;

    @FXML
    private void initialize() {
        refreshNotificationBadge();

        User user = LoginController.getLoggedInUser();
        String name = user == null ? "Patient" : user.getFullName();

        set(patientNameLabel, name);
        set(profileNameLabel, name);

        Appointment next = null;

        if (user != null) {
            String today = LocalDate.now().toString();

            for (Appointment a : new AppointmentService().getPatientAppointments(name)) {
                if (AppointmentService.REJECTED.equalsIgnoreCase(a.getStatus())
                        || AppointmentService.CANCELLED.equalsIgnoreCase(a.getStatus())
                        || AppointmentService.COMPLETED.equalsIgnoreCase(a.getStatus())
                        || a.getDate().compareTo(today) < 0) {
                    continue;
                }

                if (next == null
                        || (a.getDate() + a.getTime()).compareTo(next.getDate() + next.getTime()) < 0) {
                    next = a;
                }
            }
        }

        if (next == null) {
            set(appointmentStatusLabel, "NONE");
            set(upcomingDoctorLabel, "No appointment scheduled");
            set(upcomingSpecialtyLabel, "Book one from the Appointments page");
            set(upcomingDateLabel, "—");
            set(upcomingTimeLabel, "—");
        } else {
            set(appointmentStatusLabel, next.getStatus().replace('_', ' '));
            set(upcomingDoctorLabel, next.getDoctorName());
            set(upcomingSpecialtyLabel, next.getSpecialty());
            set(upcomingDateLabel, next.getDate());
            set(upcomingTimeLabel, next.getTime());
        }
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        showPage("patient-dashboard.fxml");
    }

    @FXML
    private void handleDoctors(ActionEvent event) {
        showPage("patient-doctors.fxml");
    }

    @FXML
    private void handleAppointments(ActionEvent event) {
        showPage("patient-appointments.fxml");
    }

    @FXML
    private void handleRequests(ActionEvent event) {
        showPage("patient-requests.fxml");
    }

    @FXML
    private void handleConsultations(ActionEvent event) {
        showPage("patient-consultation.fxml");
    }

    @FXML
    private void handleMedicalRecords(ActionEvent event) {
        showPage("patient-medical-records.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        showPage("patient-profile.fxml");
    }

    @FXML
    private void handleBookAppointment(ActionEvent event) {
        showPage("patient-appointments.fxml");
    }
}
