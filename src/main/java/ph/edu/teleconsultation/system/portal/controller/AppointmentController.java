package ph.edu.teleconsultation.system.portal.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Admin;
import ph.edu.teleconsultation.system.portal.model.AccountStatus;
import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Feedback;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.PatientType;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.model.VitalSigns;
import ph.edu.teleconsultation.system.portal.service.AppointmentService;
import ph.edu.teleconsultation.system.portal.service.FeedbackService;
import ph.edu.teleconsultation.system.portal.service.ScheduleService;

public class AppointmentController {

    @FXML private VBox bookingCard;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label summaryNumberLabel;
    @FXML private Label summaryStatusLabel;
    @FXML private Label messageLabel;

    @FXML private ComboBox<String> doctorComboBox;
    @FXML private ComboBox<String> specialtyComboBox;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextArea notesArea;

    @FXML private GridPane calendarGrid;
    @FXML private Label monthLabel;
    @FXML private Label selectedDateLabel;
    @FXML private Label availabilityLabel;

    @FXML private TableView<Appointment> appointmentTable;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> specialtyColumn;
    @FXML private TableColumn<Appointment, String> dateColumn;
    @FXML private TableColumn<Appointment, String> timeColumn;
    @FXML private TableColumn<Appointment, String> categoryColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;
    @FXML private TableColumn<Appointment, String> typeColumn;
    @FXML private Label patientTypeLabel;
    @FXML private TableColumn<Appointment, String> vitalsColumn;
    @FXML private Label patientHistoryLabel;
    @FXML private Button vitalsButton;
    @FXML private Button vitalsVisitButton;
    @FXML private TableColumn<Appointment, String> vitalsVisitColumn;
    @FXML private TextField tempField;
    @FXML private TextField heartRateField;
    @FXML private TextField bpSystolicField;
    @FXML private TextField bpDiastolicField;
    @FXML private Label vitalsStatusLabel;
    @FXML private TextField respField;

    @FXML private Button approveButton;
    @FXML private Button rejectButton;
    // Nurse: "To review" / "Rejected" tabs.
    @FXML private ToggleButton reviewTab;
    @FXML private ToggleButton rejectedTab;
    // Patient: bring back or clear a cancelled appointment.
    @FXML private Button reconsiderButton;
    @FXML private Button removeButton;
    // Patient rates a completed visit; doctor and admin read the ratings.
    @FXML private Button feedbackButton;
    @FXML private Button feedbackListButton;

    private final AppointmentService appointmentService = new AppointmentService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final FeedbackService feedbackService = new FeedbackService();

    private static String preselectedDoctor;

    private User currentUser;
    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    @FXML
    private void initialize() {
        currentUser = LoginController.getLoggedInUser();

        configureTable();
        configureCategories();
        configureDoctors();
        configureRoleView();
        setupVitalsInputs();
        loadCalendar();
        loadAppointments();
        showFeedbackReminder();
    }

    private void configureRoleView() {
        boolean patient = currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient;

        if (bookingCard != null) {
            bookingCard.setVisible(patient);
            bookingCard.setManaged(patient);
        }

        if (patient) {
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(
                    "Choose a doctor, check their live availability, and request an appointment."
            );
            if (summaryStatusLabel != null) summaryStatusLabel.setText("Booking calendar");
            showPatientType((ph.edu.teleconsultation.system.portal.model.Patient) currentUser);
        } else if (currentUser instanceof Doctor) {
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(
                    "Review appointment requests and manage your consultation schedule."
            );
            if (summaryStatusLabel != null) summaryStatusLabel.setText("Doctor appointments");
        } else {
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(
                    "Monitor and manage appointment activity across TeleConsult."
            );
            if (summaryStatusLabel != null) summaryStatusLabel.setText("Appointment monitoring");
        }

        // The workflow follows the SRS: doctor review first, secretary confirmation second.
        boolean canReview = currentUser instanceof Doctor || currentUser instanceof Secretary;
        if (approveButton != null) {
            approveButton.setVisible(canReview);
            approveButton.setManaged(canReview);
            approveButton.setText(currentUser instanceof Secretary ? "Confirm Schedule" : "Approve Request");
        }
        // Nurse review (SRS step 7): forward a complete request to the doctor, or return it.
        if (currentUser instanceof Nurse && approveButton != null) {
            approveButton.setVisible(true);
            approveButton.setManaged(true);
            approveButton.setText("Forward to Doctor");
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(
                    "New requests are listed first. Check the details and vitals, then forward to the doctor or return it.");
            if (summaryStatusLabel != null) summaryStatusLabel.setText("Requests to review");
        }

        // Only nurses see the button that asks a patient to come in for vitals.
        if (vitalsVisitButton != null) {
            vitalsVisitButton.setVisible(currentUser instanceof Nurse);
            vitalsVisitButton.setManaged(currentUser instanceof Nurse);
        }

        // Secretary can also cancel a booking she already confirmed.
        if (currentUser instanceof Secretary) {
            if (rejectButton != null) rejectButton.setText("Reject / Cancel");
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(
                    "Requests waiting for your confirmation are listed first.");
        }

        // Patients can cancel their own appointment; doctors can mark a visit as done.
        boolean patientView = currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient;
        boolean nurseView = currentUser instanceof Nurse;
        if (rejectButton != null) {
            rejectButton.setVisible(canReview || patientView || nurseView);
            rejectButton.setManaged(canReview || patientView || nurseView);
            if (patientView) rejectButton.setText("Cancel Appointment");
            if (nurseView) rejectButton.setText("Return to Patient");
        }

        // Patient: reconsider / remove a cancelled appointment.
        for (Button b : new Button[] { reconsiderButton, removeButton }) {
            if (b != null) {
                b.setVisible(patientView);
                b.setManaged(patientView);
            }
        }

        if (feedbackButton != null) {
            feedbackButton.setVisible(patientView);
            feedbackButton.setManaged(patientView);
        }
        boolean feedbackReader = currentUser instanceof Doctor || currentUser instanceof Admin;
        if (feedbackListButton != null) {
            feedbackListButton.setVisible(feedbackReader);
            feedbackListButton.setManaged(feedbackReader);
        }

        // Nurse: tabs for the review list and the returned (rejected) list.
        for (ToggleButton tab : new ToggleButton[] { reviewTab, rejectedTab }) {
            if (tab != null) {
                tab.setVisible(nurseView);
                tab.setManaged(nurseView);
            }
        }
        if (reviewTab != null && rejectedTab != null && nurseView) {
            reviewTab.setSelected(true);
            // One tab always stays picked.
            for (ToggleButton tab : new ToggleButton[] { reviewTab, rejectedTab }) {
                tab.selectedProperty().addListener((obs, was, now) -> {
                    if (!now && !reviewTab.isSelected() && !rejectedTab.isSelected()) {
                        tab.setSelected(true);
                    }
                });
            }
            reviewTab.setOnAction(e -> {
                rejectedTab.setSelected(false);
                reviewTab.setSelected(true);
                loadAppointments();
                refreshActionButtons();
            });
            rejectedTab.setOnAction(e -> {
                reviewTab.setSelected(false);
                rejectedTab.setSelected(true);
                loadAppointments();
                refreshActionButtons();
            });
        }

        // Buttons follow what is selected (e.g. "Return to Patient" becomes "Reconsider").
        if (appointmentTable != null && (patientView || nurseView)) {
            appointmentTable.getSelectionModel().getSelectedItems()
                    .addListener((javafx.collections.ListChangeListener<Appointment>) change -> refreshActionButtons());
            refreshActionButtons();
        }

        if (currentUser instanceof Doctor doctorView && approveButton != null && appointmentTable != null) {
            appointmentTable.getSelectionModel().getSelectedItems().addListener(
                    (javafx.collections.ListChangeListener<Appointment>) change -> {
                List<Appointment> picked = selectedAppointments();
                Appointment a = picked.size() == 1 ? picked.get(0) : null;
                boolean allConfirmed = !picked.isEmpty() && picked.stream().allMatch(x ->
                        AppointmentService.CONFIRMED.equalsIgnoreCase(x.getStatus()));
                boolean allCompleted = !picked.isEmpty() && picked.stream().allMatch(x ->
                        AppointmentService.COMPLETED.equalsIgnoreCase(x.getStatus()));
                approveButton.setText(allCompleted ? "Completed \u2713"
                        : allConfirmed ? "Mark Completed" : "Approve Request");
                approveButton.setDisable(allCompleted);

                // Show the patient's history so the doctor can decide with context.
                if (patientHistoryLabel != null && a != null) {
                    patientHistoryLabel.setText(a.getPatientName() + " \u2014 "
                            + appointmentService.historySummary(
                                    a.getPatientName(), doctorView.getFullName(), a));
                } else if (patientHistoryLabel != null && picked.size() > 1) {
                    patientHistoryLabel.setText(picked.size() + " appointments selected");
                }
            });
        }
    }

    private void configureTable() {
        // Ctrl+click picks single rows, Shift+click picks a range.
        if (appointmentTable != null) {
            appointmentTable.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        }
        if (patientColumn != null) patientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientName()));
        if (doctorColumn != null) doctorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDoctorName()));
        if (specialtyColumn != null) specialtyColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpecialty()));
        if (dateColumn != null) dateColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate()));
        if (timeColumn != null) timeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime()));
        if (categoryColumn != null) categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        if (statusColumn != null) statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        if (statusColumn != null) {
            // Readable, colour-coded status instead of raw text like DOCTOR_APPROVED.
            statusColumn.setCellFactory(column -> new TableCell<Appointment, String>() {
                @Override
                protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    String extra = "";
                    Appointment row = getTableRow() == null ? null : getTableRow().getItem();
                    if (row != null && AppointmentService.COMPLETED.equalsIgnoreCase(status)) {
                        Feedback rated = feedbackService.getFor(row);
                        if (rated != null) {
                            extra = "  \u2605" + rated.getRating();
                        }
                    }
                    setText(prettyStatus(status) + extra);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: " + statusColor(status) + ";");
                }
            });
        }
        // Senior / PWD / Regular, saved on the appointment when it was requested.
        if (typeColumn != null) {
            typeColumn.setCellValueFactory(data -> new SimpleStringProperty(typeText(data.getValue())));
            typeColumn.setCellFactory(column -> new TableCell<Appointment, String>() {
                @Override
                protected void updateItem(String text, boolean empty) {
                    super.updateItem(text, empty);
                    if (empty || text == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    boolean regular = text.startsWith(PatientType.REGULAR.getLabel());
                    setText(text);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: " + (regular ? "#64748B" : "#0F766E") + ";");
                }
            });
        }
        // "Oct 14, 09:00 AM  scheduled": when the patient was asked to come in for vitals (if at all).
        if (vitalsVisitColumn != null) vitalsVisitColumn.setCellValueFactory(
                data -> new SimpleStringProperty(AppointmentService.vitalsCheckText(data.getValue())));
        if (vitalsColumn != null) vitalsColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getVitals() == null ? "Not provided"
                        : data.getValue().getVitals().summary()
                        + (data.getValue().isVitalsChecked() ? "  \u2713 nurse-checked" : "  (self-reported)")));
    }

    private void configureCategories() {
        categoryComboBox.getItems().setAll(
                "General Consultation",
                "Follow-up",
                "Urgent Consultation",
                "Prescription Review",
                "Medical Advice"
        );
        categoryComboBox.getSelectionModel().selectFirst();
    }

    private void configureDoctors() {
        doctorComboBox.getItems().clear();

        for (User user : DataStore.users) {
            if (user instanceof Doctor doctor
                    && doctor.getStatus() == AccountStatus.ACTIVE) {
                doctorComboBox.getItems().add(doctor.getFullName());
            }
        }

        doctorComboBox.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    updateSpecialty();
                    selectedDate = LocalDate.now();
                    displayedMonth = YearMonth.now();
                    loadCalendar();
                    loadSelectedDate();
                }
        );

        if (preselectedDoctor != null
                && doctorComboBox.getItems().contains(preselectedDoctor)) {
            doctorComboBox.getSelectionModel().select(preselectedDoctor);
        } else if (!doctorComboBox.getItems().isEmpty()) {
            doctorComboBox.getSelectionModel().selectFirst();
        }

        preselectedDoctor = null;
    }

    /** Lets other pages (e.g. a doctor's profile) open booking with that doctor chosen. */
    public static void preselectDoctor(String doctorName) {
        preselectedDoctor = doctorName;
    }

    private void updateSpecialty() {
        specialtyComboBox.getItems().clear();

        String doctorName = doctorComboBox.getValue();
        if (doctorName == null) {
            return;
        }

        for (User user : DataStore.users) {
            if (user instanceof Doctor doctor
                    && doctor.getFullName().equalsIgnoreCase(doctorName)) {
                specialtyComboBox.getItems().add(doctor.getSpecialty());
                specialtyComboBox.getSelectionModel().selectFirst();
                return;
            }
        }
    }

    @FXML
    private void handlePreviousMonth() {
        displayedMonth = displayedMonth.minusMonths(1);
        loadCalendar();
    }

    @FXML
    private void handleNextMonth() {
        displayedMonth = displayedMonth.plusMonths(1);
        loadCalendar();
    }

    @FXML
    private void handleToday() {
        displayedMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        loadCalendar();
        loadSelectedDate();
    }

    private void loadCalendar() {
        if (calendarGrid == null) {
            return;
        }

        calendarGrid.getChildren().clear();

        String[] weekdays = {
                "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
        };

        for (int column = 0; column < 7; column++) {
            Label header = new Label(weekdays[column]);
            header.getStyleClass().add("calendar-weekday");
            calendarGrid.add(header, column, 0);
        }

        monthLabel.setText(displayedMonth.format(
                DateTimeFormatter.ofPattern("MMMM yyyy")
        ));

        String doctorName = doctorComboBox.getValue();
        if (doctorName == null) {
            return;
        }

        LocalDate first = displayedMonth.atDay(1);
        int startColumn = first.getDayOfWeek().getValue() - 1;
        int days = displayedMonth.lengthOfMonth();

        for (int day = 1; day <= days; day++) {
            LocalDate date = displayedMonth.atDay(day);
            int index = startColumn + day - 1;
            int column = index % 7;
            int row = index / 7 + 1;

            boolean available = scheduleService.isBookable(doctorName, date);

            Button cell = new Button(String.valueOf(day));
            cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            cell.setPrefHeight(62);
            cell.setMinHeight(62);
            cell.getStyleClass().add("calendar-day");
            cell.getStyleClass().add(
                    available ? "calendar-available" : "calendar-unavailable"
            );

            if (date.equals(LocalDate.now())) {
                cell.getStyleClass().add("calendar-today");
            }

            if (date.equals(selectedDate)) {
                cell.getStyleClass().add("calendar-selected");
            }

            cell.setOnAction(event -> {
                selectedDate = date;
                loadCalendar();
                loadSelectedDate();
            });

            calendarGrid.add(cell, column, row);
        }
    }

    private void loadSelectedDate() {
        String doctorName = doctorComboBox.getValue();

        if (doctorName == null) {
            availabilityLabel.setText("Select a doctor");
            return;
        }

        selectedDateLabel.setText(selectedDate.format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        ));

        boolean available = scheduleService.isBookable(
                doctorName,
                selectedDate
        );

        availabilityLabel.setText(
                available ? "AVAILABLE" : "NOT AVAILABLE"
        );

        availabilityLabel.getStyleClass().removeAll(
                "availability-green",
                "availability-red"
        );

        availabilityLabel.getStyleClass().add(
                available ? "availability-green" : "availability-red"
        );

        timeComboBox.getItems().setAll(
                scheduleService.getBookableTimes(
                        doctorName,
                        selectedDate
                )
        );

        if (!timeComboBox.getItems().isEmpty()) {
            timeComboBox.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleBookAppointment() {
        if (!(currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient)) {
            return;
        }

        String doctorName = doctorComboBox.getValue();
        String specialty = specialtyComboBox.getValue();
        String time = timeComboBox.getValue();
        String category = categoryComboBox.getValue();
        String notes = notesArea.getText().trim();

        VitalSigns selfVitals;
        try {
            selfVitals = readSelfVitals();
        } catch (IllegalArgumentException e) {
            showMessage(e.getMessage());
            return;
        }

        if (doctorName == null || specialty == null) {
            showMessage("Please select a doctor.");
            return;
        }

        String limitProblem = appointmentService.activeLimitProblem(currentUser.getFullName());
        if (limitProblem != null) {
            showMessage("You can't book more than " + AppointmentService.MAX_ACTIVE_PER_PATIENT
                    + " appointments at a time.");
            LimitReachedDialog.show(appointmentTable.getScene(),
                    appointmentService.getActiveAppointments(currentUser.getFullName()),
                    AppointmentService.MAX_ACTIVE_PER_PATIENT);
            return;
        }

        if (!scheduleService.isBookable(doctorName, selectedDate)) {
            showMessage("That date is not available. Please choose a green date.");
            return;
        }

        if (time == null || !scheduleService.getBookableTimes(
                doctorName, selectedDate).contains(time)) {
            showMessage("Please select an available time.");
            return;
        }

        Appointment appointment = new Appointment(
                DataStore.getNextAppointmentId(),
                currentUser.getFullName(),
                doctorName,
                specialty,
                selectedDate.toString(),
                time,
                category,
                notes.isEmpty() ? "Appointment request" : notes
        );

        appointment.setVitals(selfVitals);
        // Senior (from the date of birth) / PWD (declared) / Regular, kept with this request.
        if (currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient bookingPatient) {
            appointment.setPatientType(bookingPatient.getPatientType());
            appointment.setPwdIdNumber(bookingPatient.getPwdIdNumber());
        }

        if (appointmentService.createAppointment(appointment)) {
            scheduleService.bookTime(doctorName, selectedDate, time);
            String noVitalsTip = selfVitals == null
                    ? " You did not enter vitals, so the nurse may ask you to come to the clinic for a quick"
                    + " check before your appointment. Watch your Notifications."
                    : "";
            showMessage((appointment.isPriority()
                    ? "Appointment request submitted as a priority (" + appointment.getPatientType().getLabel()
                    + ") patient. A nurse will review it first, then the doctor."
                    : "Appointment request submitted. A nurse will review it first, then the doctor.")
                    + noVitalsTip);
            notesArea.clear();
            if (tempField != null) tempField.clear();
            if (heartRateField != null) heartRateField.clear();
            if (bpSystolicField != null) bpSystolicField.clear();
            if (bpDiastolicField != null) bpDiastolicField.clear();
            if (respField != null) respField.clear();
            loadSelectedDate();
            loadCalendar();
            loadAppointments();
        } else {
            showMessage("That time can no longer be booked (already requested or already passed). Please choose another.");
            loadSelectedDate();
            loadCalendar();
        }
    }

    // ------------------------------------------------ multi-select helpers

    /** Everything highlighted in the table (Ctrl+click for single rows, Shift+click for a range). */
    private List<Appointment> selectedAppointments() {
        return appointmentTable == null ? new ArrayList<>()
                : new ArrayList<>(appointmentTable.getSelectionModel().getSelectedItems());
    }

    /** For actions that only make sense for one patient (vitals): returns null and says why otherwise. */
    private Appointment singleSelected() {
        List<Appointment> picked = selectedAppointments();
        if (picked.isEmpty()) {
            showMessage("Select an appointment first.");
            return null;
        }
        if (picked.size() > 1) {
            showMessage("Select just one appointment for this action.");
            return null;
        }
        return picked.get(0);
    }

    private void refreshAll() {
        loadAppointments();
        loadCalendar();
        loadSelectedDate();
    }

    /** Collects how a batch went: how many worked, and the distinct reasons the others were skipped. */
    private static final class Outcome {
        int done;
        int skipped;
        final java.util.LinkedHashSet<String> reasons = new java.util.LinkedHashSet<>();

        /** problem == null means it worked. */
        void add(String problem) {
            if (problem == null) {
                done++;
            } else {
                skipped++;
                reasons.add(problem);
            }
        }

        String message(String doneText, String singleDone) {
            if (done + skipped == 1) {
                return done == 1 ? singleDone : reasons.iterator().next();
            }
            if (done == 0) {
                return "Nothing was changed. " + String.join(" ", reasons);
            }
            String text = done + " " + doneText;
            return skipped == 0 ? text + "."
                    : text + ", " + skipped + " skipped. " + String.join(" ", reasons);
        }
    }

    // ------------------------------------------------------ approve / forward / confirm

    @FXML
    private void handleApprove() {
        List<Appointment> picked = selectedAppointments();

        if (picked.isEmpty()) {
            showMessage("Select an appointment first.");
            return;
        }

        Outcome outcome = new Outcome();
        String single = "Done.";
        String many = "done";
        List<Appointment> completedNow = new ArrayList<>();

        for (Appointment appointment : picked) {
            if (currentUser instanceof Nurse) {
                many = "forwarded to the doctor";
                single = "Request forwarded to " + appointment.getDoctorName() + " for review.";
                if (appointmentService.forwardByNurse(appointment)) {
                    outcome.add(null);
                } else if (AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())) {
                    outcome.add("Some requests have no vitals yet. Use Update Vitals (for example after a call), "
                            + "or Vitals Visit to ask the patient to come in, then forward them.");
                } else {
                    outcome.add("Only new requests waiting for nurse review can be forwarded.");
                }

            } else if (currentUser instanceof Doctor doctor) {
                many = "approved or completed";
                if (!appointment.getDoctorName().equalsIgnoreCase(doctor.getFullName())) {
                    outcome.add("You can only review appointments assigned to you.");
                } else if (AppointmentService.CONFIRMED.equalsIgnoreCase(appointment.getStatus())) {
                    single = "Appointment marked as completed. The patient was notified and can now leave feedback.";
                    boolean completed = appointmentService.completeByDoctor(appointment, doctor.getFullName());
                    if (completed) {
                        completedNow.add(appointment);
                    }
                    outcome.add(completed
                            ? null : "Only your own confirmed appointments can be marked as completed.");
                } else if (appointmentService.approveByDoctor(appointment)) {
                    single = "Request approved. It is now waiting for secretary scheduling.";
                    outcome.add(null);
                } else {
                    outcome.add(AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())
                            ? "Some requests are still waiting for the nurse's review."
                            : "Only requests forwarded by the nurse can be approved.");
                }

            } else if (currentUser instanceof Secretary) {
                many = "confirmed";
                single = "Appointment schedule confirmed.";
                outcome.add(appointmentService.confirmBySecretary(appointment)
                        ? null : "Only doctor-approved requests can be confirmed.");

            } else {
                outcome.add("Your role cannot approve or confirm appointment schedules.");
            }
        }

        if (outcome.done > 0) {
            refreshAll();
        }
        showMessage(outcome.message(many, single));

        // The doctor gets a clear "done" card right after completing a visit.
        if (!completedNow.isEmpty()) {
            VisitCompletedDialog.show(appointmentTable.getScene(), completedNow);
        }
    }

    // ------------------------------------------------------ reject / return / cancel

    @FXML
    private void handleReject() {
        List<Appointment> picked = selectedAppointments();

        if (picked.isEmpty()) {
            showMessage("Select an appointment first.");
            return;
        }

        if (currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient patient) {
            Outcome outcome = new Outcome();
            for (Appointment appointment : picked) {
                outcome.add(appointmentService.cancelByPatient(appointment, patient.getFullName())
                        ? null : "Only your own upcoming, active appointments can be cancelled.");
            }
            if (outcome.done > 0) {
                refreshAll();
            }
            showMessage(outcome.message("appointments cancelled. The time slots are available again. "
                            + "You can reconsider or remove them from your list",
                    "Appointment cancelled. The time slot is available again. "
                            + "You can reconsider or remove it from your list."));
            return;
        }

        if (currentUser instanceof Nurse) {
            // On the "Rejected" tab this button is "Reconsider".
            if (showingRejected()) {
                Outcome outcome = new Outcome();
                for (Appointment appointment : picked) {
                    String problem = appointmentService.reconsiderByNurseProblem(appointment);
                    if (problem == null) {
                        appointmentService.reconsiderByNurse(appointment);
                    }
                    outcome.add(problem);
                }
                if (outcome.done > 0) {
                    refreshAll();
                }
                showMessage(outcome.message("requests reconsidered and back in the review list as Pending",
                        "Request reconsidered. It is back in the review list as Pending, and the patient was told."));
                return;
            }

            List<Appointment> returnable = new ArrayList<>();
            for (Appointment appointment : picked) {
                if (AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())) {
                    returnable.add(appointment);
                }
            }
            if (returnable.isEmpty()) {
                showMessage("Only new requests waiting for nurse review can be returned.");
                return;
            }
            int skippedCount = picked.size() - returnable.size();

            ReturnRequestDialog.show(appointmentTable.getScene(), returnable, reason -> {
                int returned = 0;
                for (Appointment appointment : returnable) {
                    if (appointmentService.returnByNurse(appointment, reason)) {
                        returned++;
                    }
                }
                refreshAll();
                showMessage((returned == 1 ? "Request returned." : returned + " requests returned.")
                        + " The patient was told why and the time slot is free again."
                        + (skippedCount > 0 ? " " + skippedCount + " skipped (not waiting for review)." : "")
                        + " You can find them under the Rejected tab if you want to reconsider.");
            });
            return;
        }

        if (!(currentUser instanceof Doctor) && !(currentUser instanceof Secretary)) {
            showMessage("Your role cannot reject appointment requests.");
            return;
        }

        Outcome outcome = new Outcome();
        for (Appointment appointment : picked) {
            if (currentUser instanceof Doctor doctor
                    && !appointment.getDoctorName().equalsIgnoreCase(doctor.getFullName())) {
                outcome.add("You can only review appointments assigned to you.");
            } else if (currentUser instanceof Doctor
                    && AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())) {
                outcome.add("Some requests are still waiting for the nurse's review.");
            } else if (currentUser instanceof Secretary
                    && AppointmentService.CONFIRMED.equalsIgnoreCase(appointment.getStatus())) {
                outcome.add(appointmentService.cancelBySecretary(appointment)
                        ? null : "Only upcoming confirmed appointments can be cancelled.");
            } else {
                boolean rejected = currentUser instanceof Secretary
                        ? appointmentService.rejectBySecretary(appointment)
                        : appointmentService.rejectAppointment(appointment);
                outcome.add(rejected ? null : currentUser instanceof Secretary
                        ? "Secretaries can only reject requests the doctor has already approved."
                        : "This appointment can no longer be rejected.");
            }
        }

        if (outcome.done > 0) {
            refreshAll();
        }
        showMessage(outcome.message("rejected or cancelled. The time slots are available again",
                "Appointment rejected or cancelled. The time slot is available again."));
    }

    // ------------------------------------------------ patient: reconsider / remove

    @FXML
    private void handleReconsider() {
        List<Appointment> picked = selectedAppointments();
        if (picked.isEmpty()) {
            showMessage("Select a cancelled appointment first.");
            return;
        }
        if (!(currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient patient)) {
            return;
        }

        boolean anyCancelled = picked.stream()
                .anyMatch(x -> AppointmentService.CANCELLED.equalsIgnoreCase(x.getStatus()));
        if (anyCancelled && appointmentService.activeLimitProblem(patient.getFullName()) != null) {
            showMessage("You already have " + AppointmentService.MAX_ACTIVE_PER_PATIENT + " active appointments.");
            LimitReachedDialog.show(appointmentTable.getScene(),
                    appointmentService.getActiveAppointments(patient.getFullName()),
                    AppointmentService.MAX_ACTIVE_PER_PATIENT);
            return;
        }

        Outcome outcome = new Outcome();
        for (Appointment appointment : picked) {
            String problem = appointmentService.reconsiderByPatientProblem(appointment, patient.getFullName());
            if (problem == null) {
                appointmentService.reconsiderByPatient(appointment, patient.getFullName());
            }
            outcome.add(problem);
        }
        if (outcome.done > 0) {
            refreshAll();
        }
        showMessage(outcome.message("appointments reconsidered. Your times are reserved again and a nurse will review them",
                "Appointment reconsidered. Your time is reserved again and a nurse will review it."));
    }

    @FXML
    private void handleRemove() {
        List<Appointment> picked = selectedAppointments();
        if (picked.isEmpty()) {
            showMessage("Select a cancelled appointment first.");
            return;
        }
        if (!(currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient patient)) {
            return;
        }

        Outcome outcome = new Outcome();
        for (Appointment appointment : picked) {
            outcome.add(appointmentService.removeByPatient(appointment, patient.getFullName())
                    ? null : "Only cancelled or returned appointments can be removed. Cancel them first.");
        }
        if (outcome.done > 0) {
            loadAppointments();
        }
        showMessage(outcome.message("removed from your list", "Removed from your list."));
    }

    // ------------------------------------------------------------ feedback

    /** Patients are told once, when the page opens, how many finished visits still need a rating. */
    private void showFeedbackReminder() {
        if (!(currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient patient)) {
            return;
        }
        int waiting = feedbackService.waitingCount(patient.getFullName(),
                appointmentService.getPatientAppointments(patient.getFullName()));
        if (waiting > 0) {
            showMessage(waiting == 1
                    ? "You have 1 completed visit waiting for feedback. Select it and press Give Feedback."
                    : "You have " + waiting + " completed visits waiting for feedback. "
                            + "Select one and press Give Feedback.");
        }
    }

    @FXML
    private void handleFeedback() {
        if (!(currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient patient)) {
            return;
        }
        Appointment appointment = singleSelected();
        if (appointment == null) {
            return;
        }

        String problem = feedbackService.problem(appointment, patient.getFullName());
        if (problem != null) {
            showMessage(problem);
            return;
        }

        boolean editing = feedbackService.getFor(appointment) != null;
        FeedbackDialog.show(appointmentTable.getScene(), appointment, feedbackService.getFor(appointment), choice -> {
            if (feedbackService.submit(appointment, patient.getFullName(),
                    choice.rating(), choice.tags(), choice.comment())) {
                loadAppointments();
                showMessage(editing ? "Feedback updated. Thank you!"
                        : "Thank you! Your feedback was sent to " + appointment.getDoctorName() + ".");
            } else {
                showMessage("Your feedback could not be saved.");
            }
        });
    }

    @FXML
    private void handleFeedbackList() {
        if (currentUser instanceof Doctor doctor) {
            java.util.List<Feedback> mine = feedbackService.getForDoctor(doctor.getFullName());
            FeedbackListDialog.show(appointmentTable.getScene(), "My patient feedback",
                    doctor.getFullName(), mine, false);
        } else if (currentUser instanceof Admin) {
            FeedbackListDialog.show(appointmentTable.getScene(), "All patient feedback",
                    "Ratings of 2 stars or less are highlighted for follow-up", feedbackService.getAll(), true);
        }
    }

    private boolean showingRejected() {
        return rejectedTab != null && rejectedTab.isSelected();
    }

    /** Enables and renames the action buttons to match the selected row and the nurse's tab. */
    private void refreshActionButtons() {
        List<Appointment> picked = selectedAppointments();

        if (currentUser instanceof Nurse) {
            boolean rejectedView = showingRejected();
            if (rejectButton != null) {
                rejectButton.setText(rejectedView ? "Reconsider" : "Return to Patient");
                rejectButton.getStyleClass().remove("primary-button");
                rejectButton.getStyleClass().remove("danger-button");
                rejectButton.getStyleClass().add(rejectedView ? "primary-button" : "danger-button");
            }
            if (approveButton != null) approveButton.setDisable(rejectedView);
            if (vitalsButton != null) vitalsButton.setDisable(rejectedView);
            if (vitalsVisitButton != null) vitalsVisitButton.setDisable(rejectedView);
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(rejectedView
                    ? "Requests you returned. Select one or more (Ctrl or Shift) and press Reconsider to put it back in the review list."
                    : "New requests are listed first. Check the details and vitals, then forward to the doctor or return it.");
            if (summaryStatusLabel != null) summaryStatusLabel.setText(rejectedView ? "Rejected requests" : "Requests to review");
        } else if (currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient) {
            boolean anyCancelled = picked.stream()
                    .anyMatch(x -> AppointmentService.CANCELLED.equalsIgnoreCase(x.getStatus()));
            boolean anyEnded = anyCancelled || picked.stream()
                    .anyMatch(x -> AppointmentService.REJECTED.equalsIgnoreCase(x.getStatus()));
            boolean allEnded = !picked.isEmpty() && picked.stream().allMatch(x ->
                    AppointmentService.CANCELLED.equalsIgnoreCase(x.getStatus())
                            || AppointmentService.REJECTED.equalsIgnoreCase(x.getStatus()));
            if (rejectButton != null) rejectButton.setDisable(allEnded);
            if (reconsiderButton != null) reconsiderButton.setDisable(!anyCancelled);
            if (removeButton != null) removeButton.setDisable(!anyEnded);
            if (feedbackButton != null) {
                Appointment one = picked.size() == 1 ? picked.get(0) : null;
                boolean canRate = one != null
                        && feedbackService.problem(one, currentUser.getFullName()) == null;
                feedbackButton.setDisable(!canRate);
                feedbackButton.setText(one != null && feedbackService.getFor(one) != null
                        ? "Edit Feedback" : "Give Feedback");
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadAppointments();
        loadCalendar();
        loadSelectedDate();
        showMessage("Appointments refreshed.");
    }

    private void loadAppointments() {
        if (appointmentTable == null) {
            return;
        }

        appointmentTable.getItems().clear();

        ArrayList<Appointment> appointments;

        if (currentUser instanceof Doctor doctor) {
            appointments = appointmentService.getDoctorAppointments(
                    doctor.getFullName()
            );
        } else if (currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient patient) {
            appointments = appointmentService.getPatientAppointments(
                    patient.getFullName()
            );
        } else {
            appointments = new ArrayList<>(
                    appointmentService.getAllAppointments()
            );
        }

        if (currentUser instanceof Nurse) {
            // Returned requests leave the review list; they live under the "Rejected" tab.
            boolean rejectedView = showingRejected();
            appointments.removeIf(a -> AppointmentService.REJECTED.equalsIgnoreCase(a.getStatus()) != rejectedView);
        }

        if (currentUser instanceof Secretary) {
            appointments.sort(AppointmentService.SECRETARY_FIRST);
        } else if (currentUser instanceof Nurse) {
            appointments.sort(AppointmentService.NURSE_FIRST);
        } else if (currentUser instanceof Doctor) {
            appointments.sort(AppointmentService.DOCTOR_FIRST);
        } else if (!(currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient)) {
            appointments.sort(AppointmentService.BY_SCHEDULE);
        }

        appointmentTable.getItems().addAll(appointments);
        summaryNumberLabel.setText(String.valueOf(appointments.size()));

        if (currentUser instanceof ph.edu.teleconsultation.system.portal.model.Patient patientUser && summaryStatusLabel != null) {
            summaryStatusLabel.setText("Booking calendar \u00B7 " + appointmentService.activeCount(patientUser.getFullName())
                    + " of " + AppointmentService.MAX_ACTIVE_PER_PATIENT + " active appointments");
        }
    }

    /** Nurse checks or enters vitals herself (for example after calling a patient without a device). */
    @FXML
    private void handleRecordVitals() {
        if (!(currentUser instanceof Nurse)) {
            return;
        }

        Appointment appointment = singleSelected();
        if (appointment == null) {
            return;
        }

        VitalsDialog.show(appointment.getPatientName(), appointment.getVitals()).ifPresent(vitals -> {
            if (appointmentService.recordVitals(appointment, vitals)) {
                loadAppointments();
                showMessage("Vitals saved for " + appointment.getPatientName() + ".");
            } else {
                showMessage("Vitals can only be updated on active requests.");
            }
        });
    }

    /**
     * The patient has no home device: the nurse picks a day and time before the appointment
     * for the patient to come to the clinic and have vitals taken.
     */
    @FXML
    private void handleScheduleVitalsVisit() {
        if (!(currentUser instanceof Nurse)) {
            return;
        }

        Appointment appointment = singleSelected();
        if (appointment == null) {
            return;
        }
        String problem = appointmentService.vitalsVisitProblem(appointment);

        if (problem != null) {
            showMessage(problem);
            return;
        }

        if (appointmentService.firstVitalsVisitDate(appointment) == null) {
            showMessage("There is no time left before this appointment for a clinic check. "
                    + "Use Update Vitals (call the patient) or return the request.");
            return;
        }

        // The card opens over the page; this runs only when the nurse presses "Send to patient".
        VitalsVisitDialog.show(appointmentTable.getScene(), appointment, appointmentService, choice -> {
            if (appointmentService.scheduleVitalsCheck(
                    appointment, choice.date(), choice.time(), choice.note())) {
                loadAppointments();
                showMessage(appointment.getPatientName() + " was asked to come in on " + choice.date()
                        + " at " + choice.time() + ". Record the vitals when they arrive, then forward the request.");
            } else {
                showMessage(appointmentService.vitalsVisitProblem(
                        appointment, choice.date(), choice.time()));
            }
        });
    }

    /** Vitals the patient typed in: all blank means none, otherwise every box must be filled and valid. */
    private VitalSigns readSelfVitals() {
        String t = fieldText(tempField);
        String h = fieldText(heartRateField);
        String top = fieldText(bpSystolicField);
        String bottom = fieldText(bpDiastolicField);
        String r = fieldText(respField);

        if (t.isEmpty() && h.isEmpty() && top.isEmpty() && bottom.isEmpty() && r.isEmpty()) {
            return null;
        }

        refreshVitalsFeedback();

        List<String> missing = VitalsInput.missing(t, h, top, bottom, r);

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Your vitals are not complete yet. Still missing: "
                    + String.join(", ", missing)
                    + ". Fill them in, or press Clear to skip them and let the nurse check you.");
        }

        for (VitalsInput.Check check : List.of(
                VitalsInput.temperature(t), VitalsInput.pulse(h),
                VitalsInput.systolic(top), VitalsInput.diastolic(bottom, top),
                VitalsInput.breathing(r))) {
            if (check.isBad()) {
                throw new IllegalArgumentException(check.message());
            }
        }

        double temperature = VitalsInput.temperatureCelsius(t);
        int heartRate = Integer.parseInt(h);
        int systolic = Integer.parseInt(top);
        int diastolic = Integer.parseInt(bottom);
        int breathing = Integer.parseInt(r);

        String problem = VitalSigns.check(temperature, heartRate, systolic, diastolic, breathing);
        if (problem != null) {
            throw new IllegalArgumentException(problem);
        }

        return new VitalSigns(temperature, heartRate, systolic, diastolic, breathing);
    }

    // ------------------------------------------------- easy vitals form (patient)

    @FXML
    private void handleClearVitals() {
        for (TextField field : vitalFields()) {
            field.clear();
        }

        refreshVitalsFeedback();
    }

    private List<TextField> vitalFields() {
        return List.of(tempField, heartRateField, bpSystolicField, bpDiastolicField, respField);
    }

    /** Digits only, auto-jump from the top to the bottom blood pressure box, live green/red feedback. */
    private void setupVitalsInputs() {
        if (tempField == null || heartRateField == null || bpSystolicField == null
                || bpDiastolicField == null || respField == null || vitalsStatusLabel == null) {
            return;   // this page has no vitals form (other roles)
        }

        limitInput(tempField, "\\d{0,3}([.,]\\d?)?");
        limitInput(heartRateField, "\\d{0,3}");
        limitInput(bpDiastolicField, "\\d{0,3}");
        limitInput(respField, "\\d{0,2}");

        Pattern digits = Pattern.compile("\\d{0,3}");
        Pattern pasted = Pattern.compile("\\s*(\\d{2,3})\\s*[/\\\\\\s-]\\s*(\\d{2,3})\\s*");

        bpSystolicField.setTextFormatter(new TextFormatter<String>(change -> {
            String added = change.getText();
            Matcher whole = pasted.matcher(added);

            if (added.length() > 3 && whole.matches()) {        // pasted "120/80"
                String top = whole.group(1);
                String bottom = whole.group(2);
                Platform.runLater(() -> {
                    bpSystolicField.setText(top);
                    bpDiastolicField.setText(bottom);
                });
                return null;
            }

            if (added.matches("[/\\\\\\s-]")) {                      // typed "/" : go to the bottom number
                Platform.runLater(bpDiastolicField::requestFocus);
                return null;
            }

            return digits.matcher(change.getControlNewText()).matches() ? change : null;
        }));

        bpSystolicField.textProperty().addListener((observable, was, now) -> {
            if (now.length() == 3 && was.length() < 3 && bpSystolicField.isFocused()) {
                bpDiastolicField.requestFocus();
            }
        });

        for (TextField field : vitalFields()) {
            field.textProperty().addListener((observable, was, now) -> refreshVitalsFeedback());
            field.focusedProperty().addListener((observable, was, now) -> refreshVitalsFeedback());
        }

        refreshVitalsFeedback();
    }

    private static void limitInput(TextField field, String allowed) {
        Pattern pattern = Pattern.compile(allowed);

        field.setTextFormatter(new TextFormatter<String>(change ->
                pattern.matcher(change.getControlNewText()).matches() ? change : null));
    }

    /** Colours each box and writes one plain-language line about where the patient stands. */
    private void refreshVitalsFeedback() {
        if (vitalsStatusLabel == null || tempField == null) {
            return;
        }

        VitalsInput.Check temp = VitalsInput.temperature(tempField.getText());
        VitalsInput.Check pulse = VitalsInput.pulse(heartRateField.getText());
        VitalsInput.Check top = VitalsInput.systolic(bpSystolicField.getText());
        VitalsInput.Check bottom = VitalsInput.diastolic(bpDiastolicField.getText(), bpSystolicField.getText());
        VitalsInput.Check breath = VitalsInput.breathing(respField.getText());

        markBox(tempField, temp);
        markBox(heartRateField, pulse);
        markBox(bpSystolicField, top);
        markBox(bpDiastolicField, bottom);
        markBox(respField, breath);

        // A box still being typed in is never called wrong yet (typing "3" on the way to "36.5").
        String problem = null;
        boolean typing = false;

        TextField[] boxes = {tempField, heartRateField, bpSystolicField, bpDiastolicField, respField};
        VitalsInput.Check[] checks = {temp, pulse, top, bottom, breath};

        for (int i = 0; i < boxes.length; i++) {
            if (checks[i].isBad()) {
                if (boxes[i].isFocused()) {
                    typing = true;
                } else if (problem == null) {
                    problem = checks[i].message();
                }
            }
        }

        // Count the five boxes the patient can see, and name what is still missing.
        int filled = VitalsInput.filledBoxes(fieldText(tempField), fieldText(heartRateField),
                fieldText(bpSystolicField), fieldText(bpDiastolicField), fieldText(respField));

        List<String> missing = VitalsInput.missing(fieldText(tempField), fieldText(heartRateField),
                fieldText(bpSystolicField), fieldText(bpDiastolicField), fieldText(respField));

        String text;
        String style;

        if (problem != null) {
            text = problem;
            style = "vitals-status-bad";
        } else if (typing) {
            text = filled + " of 5 filled in...";
            style = "vitals-status-muted";
        } else if (temp.state() == VitalsInput.State.OK && temp.message() != null) {
            text = temp.message();
            style = "vitals-status-ok";
        } else if (filled == 0) {
            text = "Nothing entered. That's fine. If the nurse needs your vitals, she will send you a time to come to the clinic before your appointment.";
            style = "vitals-status-muted";
        } else if (!missing.isEmpty()) {
            text = filled + " of 5 filled in. Still missing: " + String.join(", ", missing)
                    + ". Finish them, or press Clear to skip.";
            style = "vitals-status-warn";
        } else {
            text = "All set. Your vitals will be sent with your request.";
            style = "vitals-status-ok";
        }

        vitalsStatusLabel.setText(text);
        vitalsStatusLabel.getStyleClass().removeAll("vitals-status-muted", "vitals-status-warn",
                "vitals-status-bad", "vitals-status-ok");
        vitalsStatusLabel.getStyleClass().add(style);
    }

    private static void markBox(TextField field, VitalsInput.Check check) {
        field.getStyleClass().removeAll("vital-input-ok", "vital-input-bad");

        if (check.state() == VitalsInput.State.OK) {
            field.getStyleClass().add("vital-input-ok");
        } else if (check.isBad() && !field.isFocused()) {
            field.getStyleClass().add("vital-input-bad");
        }
    }

    private static String fieldText(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    /** "Senior", "PWD  (ID PWD-2026-0001)", "Regular" ... shown in the Type column. */
    private static String typeText(Appointment appointment) {
        String id = appointment.getPwdIdNumber();
        return appointment.getPatientType().getLabel()
                + (id == null || id.isBlank() ? "" : "  (ID " + id + ")");
    }

    /** Booking page: tells the patient how they will be served (priority or regular). */
    private void showPatientType(ph.edu.teleconsultation.system.portal.model.Patient patient) {
        if (patientTypeLabel == null) {
            return;
        }

        PatientType type = patient.getPatientType();
        String text = "Your patient type: " + type.getLabel()
                + (type == PatientType.SENIOR || type == PatientType.SENIOR_PWD
                ? " (age " + patient.getAge() + ")" : "")
                + (type.isPriority()
                ? "  \u2022  priority review: your request is checked first"
                : "  \u2022  regular review");

        patientTypeLabel.setText(text);
        patientTypeLabel.getStyleClass().removeAll("patient-type-priority", "patient-type-regular");
        patientTypeLabel.getStyleClass().add(
                type.isPriority() ? "patient-type-priority" : "patient-type-regular");
    }

    private static String prettyStatus(String status) {
        String text = status.replace('_', ' ').toLowerCase();
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String statusColor(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED", "COMPLETED" -> "#1E8E5A";
            case "REJECTED", "CANCELLED" -> "#B42D2D";
            case "DOCTOR_APPROVED" -> "#2164DF";
            case "FOR_DOCTOR_REVIEW" -> "#6B46C1";
            default -> "#B7791F";
        };
    }

    private void showMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }
}
