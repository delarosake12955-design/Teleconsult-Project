package ph.edu.liceo.portal.controller;

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
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.Admin;
import ph.edu.liceo.portal.model.AccountStatus;
import ph.edu.liceo.portal.model.Appointment;
import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.Nurse;
import ph.edu.liceo.portal.model.PatientType;
import ph.edu.liceo.portal.model.Secretary;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.model.VitalSigns;
import ph.edu.liceo.portal.service.AppointmentService;
import ph.edu.liceo.portal.service.ScheduleService;

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
    @FXML private TextField tempField;
    @FXML private TextField heartRateField;
    @FXML private TextField bpSystolicField;
    @FXML private TextField bpDiastolicField;
    @FXML private Label vitalsStatusLabel;
    @FXML private TextField respField;

    @FXML private Button approveButton;
    @FXML private Button rejectButton;

    private final AppointmentService appointmentService = new AppointmentService();
    private final ScheduleService scheduleService = new ScheduleService();

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
    }

    private void configureRoleView() {
        boolean patient = currentUser instanceof ph.edu.liceo.portal.model.Patient;

        if (bookingCard != null) {
            bookingCard.setVisible(patient);
            bookingCard.setManaged(patient);
        }

        if (patient) {
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(
                    "Choose a doctor, check their live availability, and request an appointment."
            );
            if (summaryStatusLabel != null) summaryStatusLabel.setText("Booking calendar");
            showPatientType((ph.edu.liceo.portal.model.Patient) currentUser);
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

        // Secretary can also cancel a booking she already confirmed.
        if (currentUser instanceof Secretary) {
            if (rejectButton != null) rejectButton.setText("Reject / Cancel");
            if (pageSubtitleLabel != null) pageSubtitleLabel.setText(
                    "Requests waiting for your confirmation are listed first.");
        }

        // Patients can cancel their own appointment; doctors can mark a visit as done.
        boolean patientView = currentUser instanceof ph.edu.liceo.portal.model.Patient;
        boolean nurseView = currentUser instanceof Nurse;
        if (rejectButton != null) {
            rejectButton.setVisible(canReview || patientView || nurseView);
            rejectButton.setManaged(canReview || patientView || nurseView);
            if (patientView) rejectButton.setText("Cancel Appointment");
            if (nurseView) rejectButton.setText("Return to Patient");
        }

        if (currentUser instanceof Doctor doctorView && approveButton != null && appointmentTable != null) {
            appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, a) -> {
                approveButton.setText(a != null && AppointmentService.CONFIRMED
                        .equalsIgnoreCase(a.getStatus()) ? "Mark Completed" : "Approve Request");

                // Show the patient's history so the doctor can decide with context.
                if (patientHistoryLabel != null && a != null) {
                    patientHistoryLabel.setText(a.getPatientName() + " \u2014 "
                            + appointmentService.historySummary(
                                    a.getPatientName(), doctorView.getFullName(), a));
                }
            });
        }
    }

    private void configureTable() {
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
                    setText(prettyStatus(status));
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
        if (!(currentUser instanceof ph.edu.liceo.portal.model.Patient)) {
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
        if (currentUser instanceof ph.edu.liceo.portal.model.Patient bookingPatient) {
            appointment.setPatientType(bookingPatient.getPatientType());
            appointment.setPwdIdNumber(bookingPatient.getPwdIdNumber());
        }

        if (appointmentService.createAppointment(appointment)) {
            scheduleService.bookTime(doctorName, selectedDate, time);
            showMessage(appointment.isPriority()
                    ? "Appointment request submitted as a priority (" + appointment.getPatientType().getLabel()
                    + ") patient. A nurse will review it first, then the doctor."
                    : "Appointment request submitted. A nurse will review it first, then the doctor.");
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

    @FXML
    private void handleApprove() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();

        if (appointment == null) {
            showMessage("Select an appointment first.");
            return;
        }

        if (currentUser instanceof Nurse) {
            if (appointmentService.forwardByNurse(appointment)) {
                loadAppointments();
                showMessage("Request forwarded to " + appointment.getDoctorName() + " for review.");
            } else if (AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())) {
                showMessage("This request has no vitals yet. Use Update Vitals first, then forward it.");
            } else {
                showMessage("Only new requests waiting for nurse review can be forwarded.");
            }
            return;
        }

        if (currentUser instanceof Doctor doctor) {
            if (!appointment.getDoctorName().equalsIgnoreCase(doctor.getFullName())) {
                showMessage("You can only review appointments assigned to you.");
                return;
            }
            if (AppointmentService.CONFIRMED.equalsIgnoreCase(appointment.getStatus())) {
                if (appointmentService.completeByDoctor(appointment, doctor.getFullName())) {
                    loadAppointments();
                    showMessage("Appointment marked as completed.");
                } else {
                    showMessage("An appointment can be completed on or after its date.");
                }
                return;
            }
            if (appointmentService.approveByDoctor(appointment)) {
                loadAppointments();
                showMessage("Request approved. It is now waiting for secretary scheduling.");
            } else {
                showMessage(AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())
                        ? "This request is still waiting for the nurse's review."
                        : "Only requests forwarded by the nurse can be approved.");
            }
        } else if (currentUser instanceof Secretary) {
            if (appointmentService.confirmBySecretary(appointment)) {
                loadAppointments();
                showMessage("Appointment schedule confirmed.");
            } else {
                showMessage("Only doctor-approved requests can be confirmed.");
            }
        } else {
            showMessage("Your role cannot approve or confirm appointment schedules.");
        }
    }

    @FXML
    private void handleReject() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();

        if (appointment == null) {
            showMessage("Select an appointment first.");
            return;
        }

        if (currentUser instanceof ph.edu.liceo.portal.model.Patient patient) {
            if (appointmentService.cancelByPatient(appointment, patient.getFullName())) {
                loadAppointments();
                loadCalendar();
                loadSelectedDate();
                showMessage("Appointment cancelled. The time slot is available again.");
            } else {
                showMessage("Only your own upcoming, active appointments can be cancelled.");
            }
            return;
        }

        if (currentUser instanceof Nurse) {
            if (!AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())) {
                showMessage("Only new requests waiting for nurse review can be returned.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Return request");
            dialog.setHeaderText("Return " + appointment.getPatientName() + "'s request");
            dialog.setContentText("Reason for the patient:");
            dialog.showAndWait().ifPresent(reason -> {
                if (appointmentService.returnByNurse(appointment, reason)) {
                    loadAppointments();
                    loadCalendar();
                    loadSelectedDate();
                    showMessage("Request returned. The patient was told why and the time slot is free again.");
                }
            });
            return;
        }

        if (!(currentUser instanceof Doctor) && !(currentUser instanceof Secretary)) {
            showMessage("Your role cannot reject appointment requests.");
            return;
        }
        if (currentUser instanceof Doctor doctor
                && !appointment.getDoctorName().equalsIgnoreCase(doctor.getFullName())) {
            showMessage("You can only review appointments assigned to you.");
            return;
        }
        if (currentUser instanceof Doctor
                && AppointmentService.PENDING.equalsIgnoreCase(appointment.getStatus())) {
            showMessage("This request is still waiting for the nurse's review.");
            return;
        }
        if (currentUser instanceof Secretary
                && AppointmentService.CONFIRMED.equalsIgnoreCase(appointment.getStatus())) {
            if (appointmentService.cancelBySecretary(appointment)) {
                loadAppointments();
                loadCalendar();
                loadSelectedDate();
                showMessage("Confirmed appointment cancelled. The patient and doctor were notified.");
            } else {
                showMessage("Only upcoming confirmed appointments can be cancelled.");
            }
            return;
        }

        boolean rejected = currentUser instanceof Secretary
                ? appointmentService.rejectBySecretary(appointment)
                : appointmentService.rejectAppointment(appointment);

        if (rejected) {
            loadAppointments();
            loadCalendar();
            loadSelectedDate();
            showMessage("Appointment rejected. The time slot is available again.");
        } else {
            showMessage(currentUser instanceof Secretary
                    ? "Secretaries can only reject requests the doctor has already approved."
                    : "This appointment can no longer be rejected.");
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
        } else if (currentUser instanceof ph.edu.liceo.portal.model.Patient patient) {
            appointments = appointmentService.getPatientAppointments(
                    patient.getFullName()
            );
        } else {
            appointments = new ArrayList<>(
                    appointmentService.getAllAppointments()
            );
        }

        if (currentUser instanceof Secretary) {
            appointments.sort(AppointmentService.SECRETARY_FIRST);
        } else if (currentUser instanceof Nurse) {
            appointments.sort(AppointmentService.NURSE_FIRST);
        } else if (currentUser instanceof Doctor) {
            appointments.sort(AppointmentService.DOCTOR_FIRST);
        } else if (!(currentUser instanceof ph.edu.liceo.portal.model.Patient)) {
            appointments.sort(AppointmentService.BY_SCHEDULE);
        }

        appointmentTable.getItems().addAll(appointments);
        summaryNumberLabel.setText(String.valueOf(appointments.size()));
    }

    /** Nurse checks or enters vitals herself (for example after calling a patient without a device). */
    @FXML
    private void handleRecordVitals() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();

        if (!(currentUser instanceof Nurse)) {
            return;
        }

        if (appointment == null) {
            showMessage("Select an appointment first.");
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

    /** Vitals the patient typed in: all blank means none, otherwise all four must be filled and valid. */
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

        int filled = (t.isEmpty() ? 0 : 1) + (h.isEmpty() ? 0 : 1)
                + (top.isEmpty() || bottom.isEmpty() ? 0 : 1) + (r.isEmpty() ? 0 : 1);

        if (filled < 4) {
            throw new IllegalArgumentException("Your vitals are " + filled
                    + " of 4 done. Fill in the rest, or press Clear to skip them and let the nurse check you.");
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

        int filled = (fieldText(tempField).isEmpty() ? 0 : 1) + (fieldText(heartRateField).isEmpty() ? 0 : 1)
                + (fieldText(bpSystolicField).isEmpty() || fieldText(bpDiastolicField).isEmpty() ? 0 : 1)
                + (fieldText(respField).isEmpty() ? 0 : 1);

        String text;
        String style;

        if (problem != null) {
            text = problem;
            style = "vitals-status-bad";
        } else if (typing) {
            text = filled + " of 4 filled in...";
            style = "vitals-status-muted";
        } else if (temp.state() == VitalsInput.State.OK && temp.message() != null) {
            text = temp.message();
            style = "vitals-status-ok";
        } else if (filled == 0) {
            text = "Nothing entered. That's fine, a nurse will take your vitals for you.";
            style = "vitals-status-muted";
        } else if (filled < 4) {
            text = filled + " of 4 filled in. Finish the rest, or press Clear to skip.";
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
    private void showPatientType(ph.edu.liceo.portal.model.Patient patient) {
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
