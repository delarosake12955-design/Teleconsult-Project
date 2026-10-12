package ph.edu.liceo.portal.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.AccountStatus;
import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.Patient;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.service.ScheduleService;

/**
 * Serves both the doctor directory pages and the doctor profile page.
 */
public class DoctorController extends PageController {

    private static Doctor selectedDoctor;

    // Directory page
    @FXML private TextField searchField;
    @FXML private Label doctorCountLabel;
    @FXML private Label specialtyCountLabel;
    @FXML private VBox doctorList;

    // Profile page
    @FXML private Label avatarLabel;
    @FXML private Label bioLabel;
    @FXML private Label doctorNameLabel;
    @FXML private Label specialtyLabel;
    @FXML private Label licenseLabel;
    @FXML private Label specialtyValueLabel;
    @FXML private Label licenseValueLabel;
    @FXML private Label availableDatesLabel;
    @FXML private Label availableTimesLabel;
    @FXML private Button bookAppointmentButton;

    private final ScheduleService scheduleService = new ScheduleService();

    @FXML
    private void initialize() {
        if (doctorList != null) {
            loadDoctors("");
        }

        if (doctorNameLabel != null && doctorList == null) {
            loadProfile();
        }
    }

    // ---------------------------------------------------------------- directory

    @FXML
    private void handleSearch() {
        loadDoctors(searchField == null ? "" : searchField.getText().trim());
    }

    private void loadDoctors(String query) {
        doctorList.getChildren().clear();

        String needle = query.toLowerCase();
        Set<String> specialties = new HashSet<>();
        int count = 0;

        for (Doctor doctor : activeDoctors()) {
            String name = doctor.getFullName();
            String specialty = valueOrDash(doctor.getSpecialty(), "General Medicine");

            if (!needle.isEmpty()
                    && !name.toLowerCase().contains(needle)
                    && !specialty.toLowerCase().contains(needle)) {
                continue;
            }

            doctorList.getChildren().add(buildCard(doctor, specialty));
            specialties.add(specialty.toLowerCase());
            count++;
        }

        if (count == 0) {
            Label empty = new Label("No doctors match your search.");
            empty.getStyleClass().add("empty-label");
            doctorList.getChildren().add(empty);
        }

        doctorCountLabel.setText(String.valueOf(count));
        specialtyCountLabel.setText(String.valueOf(specialties.size()));
    }

    private HBox buildCard(Doctor doctor, String specialty) {
        HBox card = new HBox(18);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("doctor-card");

        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(doctor.getFullName());
        name.getStyleClass().add("doctor-name");

        Label spec = new Label(specialty);
        spec.getStyleClass().add("specialty-label");

        Label license = new Label(
                "License No. " + valueOrDash(doctor.getLicenseNumber(), "—"));
        license.getStyleClass().add("license-label");

        info.getChildren().addAll(name, spec, license);

        Button view = new Button("View Profile");
        view.getStyleClass().add("profile-button");
        view.setOnAction(event -> {
            selectedDoctor = doctor;
            Navigator.open(view, "doctor-profile.fxml");
        });

        card.getChildren().addAll(info, view);
        return card;
    }

    private ArrayList<Doctor> activeDoctors() {
        ArrayList<Doctor> doctors = new ArrayList<>();

        for (User user : DataStore.users) {
            if (user instanceof Doctor doctor
                    && doctor.getStatus() == AccountStatus.ACTIVE) {
                doctors.add(doctor);
            }
        }

        return doctors;
    }

    // ------------------------------------------------------------------ profile

    private void loadProfile() {
        Doctor doctor = selectedDoctor;

        if (doctor == null) {
            ArrayList<Doctor> doctors = activeDoctors();
            doctor = doctors.isEmpty() ? null : doctors.get(0);
        }

        if (doctor == null) {
            doctorNameLabel.setText("No doctor selected");
            specialtyLabel.setText("");
            licenseLabel.setText("");
            if (bookAppointmentButton != null) {
                bookAppointmentButton.setDisable(true);
            }
            return;
        }

        selectedDoctor = doctor;

        String specialty = valueOrDash(doctor.getSpecialty(), "General Medicine");
        String license = valueOrDash(doctor.getLicenseNumber(), "—");

        doctorNameLabel.setText(doctor.getFullName());
        specialtyLabel.setText(specialty);
        licenseLabel.setText("License No. " + license);
        specialtyValueLabel.setText(specialty);
        licenseValueLabel.setText(license);
        avatarLabel.setText(initials(doctor.getFullName()));
        ProfileStyle.apply(avatarLabel, doctor.getThemeKey(), doctor.getPhoto());

        if (bioLabel != null) {
            boolean hasBio = !doctor.getBio().isBlank();
            bioLabel.setText(doctor.getBio());
            bioLabel.setVisible(hasBio);
            bioLabel.setManaged(hasBio);
        }

        loadAvailability(doctor.getFullName());

        // Only patients can book; staff just view the profile.
        boolean canBook = LoginController.getLoggedInUser() instanceof Patient;
        if (bookAppointmentButton != null) {
            bookAppointmentButton.setVisible(canBook);
            bookAppointmentButton.setManaged(canBook);
        }
    }

    private void loadAvailability(String doctorName) {
        ArrayList<String> dates = new ArrayList<>();
        Set<String> times = new java.util.TreeSet<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 30 && dates.size() < 5; i++) {
            LocalDate date = today.plusDays(i);

            if (scheduleService.isBookable(doctorName, date)) {
                dates.add(date.toString());
                times.addAll(scheduleService.getBookableTimes(doctorName, date));
            }
        }

        availableDatesLabel.setText(dates.isEmpty()
                ? "No upcoming availability"
                : String.join("  •  ", dates));

        availableTimesLabel.setText(times.isEmpty()
                ? "No time slots published yet"
                : String.join("  •  ", times));
    }

    @FXML
    private void handleBookAppointment(ActionEvent event) {
        if (selectedDoctor != null) {
            AppointmentController.preselectDoctor(selectedDoctor.getFullName());
        }
        go(event, "appointments");
    }

    /** Back button on the profile page. */
    @Override
    @FXML
    protected void handleDoctors(ActionEvent event) {
        go(event, "doctors");
    }

    @FXML
    private void handleDoctorProfile(ActionEvent event) {
        Navigator.open(source(event), "doctor-profile.fxml");
    }

    // ------------------------------------------------------------------ helpers

    private String valueOrDash(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String initials(String name) {
        StringBuilder builder = new StringBuilder();

        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && Character.isLetter(part.charAt(0))
                    && !part.endsWith(".")) {
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
            if (builder.length() == 2) {
                break;
            }
        }

        return builder.length() == 0 ? "DR" : builder.toString();
    }
}
