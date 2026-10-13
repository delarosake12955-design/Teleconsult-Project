package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.service.AppointmentService;

public class DoctorDashboardController extends DashboardShell {



    @FXML
    private Label doctorNameLabel;

    @FXML
    private Label profileNameLabel;

    @FXML
    private Label specialtyLabel;

    @FXML
    private Label appointmentCountLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Label patientCountLabel;

    @FXML
    private Label requestPatientLabel;

    @FXML
    private Label requestDetailsLabel;

    @FXML
    private Label requestStatusLabel;

    @FXML
    private Label scheduleDateLabel;

    @FXML
    private Label scheduleCountLabel;

    private AppointmentService appointmentService = new AppointmentService();

    private Doctor currentDoctor;

    @FXML
    private void initialize() {
        refreshNotificationBadge();

        loadDoctorData();
        loadDashboardData();
    }

    private void loadDoctorData() {

        User loggedInUser = LoginController.getLoggedInUser();

        if (loggedInUser instanceof Doctor) {
            currentDoctor = (Doctor) loggedInUser;

            doctorNameLabel.setText(currentDoctor.getFullName());
            profileNameLabel.setText(currentDoctor.getFullName());
            specialtyLabel.setText(currentDoctor.getSpecialty());

        } else {
            doctorNameLabel.setText("Doctor");
            profileNameLabel.setText("Doctor");
            specialtyLabel.setText("Medical Specialist");
        }
    }

    private void loadDashboardData() {

        if (currentDoctor == null) {
            appointmentCountLabel.setText("0");
            pendingCountLabel.setText("0");
            patientCountLabel.setText("0");

            requestPatientLabel.setText("No requests");
            requestDetailsLabel.setText("No pending appointment requests");
            requestStatusLabel.setText("NONE");

            scheduleDateLabel.setText("Today");
            scheduleCountLabel.setText("0 appointments");

            return;
        }

        ArrayList<Appointment> appointments =
                appointmentService.getDoctorAppointments(
                        currentDoctor.getFullName()
                );

        appointmentCountLabel.setText(
                String.valueOf(appointments.size())
        );

        int pending = 0;

        java.util.HashSet<String> patients = new java.util.HashSet<>();

        for (Appointment appointment : appointments) {

            if (appointment.getStatus().equalsIgnoreCase(AppointmentService.FOR_DOCTOR_REVIEW)) {
                pending++;
            }

            patients.add(appointment.getPatientName());
        }

        pendingCountLabel.setText(String.valueOf(pending));
        patientCountLabel.setText(String.valueOf(patients.size()));

        loadPendingRequest(appointments);
        loadTodaySchedule(appointments);
    }

    private void loadPendingRequest(ArrayList<Appointment> appointments) {

        // The waiting list is a FIFO queue: the head is the oldest request.
        java.util.Queue<Appointment> queue =
                appointmentService.getPendingQueue(currentDoctor.getFullName());

        Appointment next = queue.peek();

        if (next != null) {

            requestPatientLabel.setText(next.getPatientName()
                    + (next.isPriority() ? "  [" + next.getPatientType().getLabel() + "]" : ""));

            requestDetailsLabel.setText(
                    next.getSpecialty()
                            + " • "
                            + next.getDate()
                            + " • "
                            + next.getTime()
                            + (queue.size() > 1
                            ? " • +" + (queue.size() - 1) + " more waiting"
                            : "")
            );

            requestStatusLabel.setText(next.getStatus());

            return;
        }

        requestPatientLabel.setText("No pending requests");
        requestDetailsLabel.setText("You have no new appointment requests");
        requestStatusLabel.setText("NONE");
    }

    private void loadTodaySchedule(ArrayList<Appointment> appointments) {

        String today = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        int todayCount = 0;

        for (Appointment appointment : appointments) {

            if (appointment.getDate().equals(today)
                    && (appointment.getStatus().equalsIgnoreCase(AppointmentService.DOCTOR_APPROVED)
                    || appointment.getStatus().equalsIgnoreCase(AppointmentService.CONFIRMED))) {

                todayCount++;
            }
        }

        scheduleDateLabel.setText("Today");
        scheduleCountLabel.setText(
                todayCount + (todayCount == 1
                        ? " appointment"
                        : " appointments")
        );
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        showPage("doctor-dashboard.fxml");
    }

    @FXML
    private void handleAppointments(ActionEvent event) {
        showPage("doctor-appointments.fxml");
    }

    @FXML
    private void handlePatients(ActionEvent event) {
        showPage("doctor-patients.fxml");
    }

    @FXML
    private void handleConsultations(ActionEvent event) {
        showPage("doctor-consultation.fxml");
    }

    @FXML
    private void handleMedicalRecords(ActionEvent event) {
        showPage("doctor-medical-records.fxml");
    }

    @FXML
    private void handleSchedule(ActionEvent event) {
        showPage("doctor-schedule.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        showPage("doctor-profile-page.fxml");
    }


}
