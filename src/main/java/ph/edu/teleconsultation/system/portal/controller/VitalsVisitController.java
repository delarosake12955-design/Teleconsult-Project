package ph.edu.teleconsultation.system.portal.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;

import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.service.AppointmentService;

/**
 * The "Vitals check-in" card: the nurse taps a day and a time chip, sees the plan on a small
 * timeline (vitals check, then the appointment) and sends it to the patient.
 */
public class VitalsVisitController {

    /** More days than this just push the time chips off screen; the nurse can re-open it later. */
    private static final int MAX_DAY_CHIPS = 10;

    private static final DateTimeFormatter LONG_DAY =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter WEEKDAY =
            DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_DAY =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    @FXML private Label patientLabel;
    @FXML private Label typeLabel;
    @FXML private Label checkSummaryLabel;
    @FXML private Label whereSummaryLabel;
    @FXML private Label appointmentLabel;
    @FXML private Label doctorLabel;
    @FXML private Label errorLabel;
    @FXML private Label hintLabel;
    @FXML private FlowPane dayChips;
    @FXML private FlowPane timeChips;
    @FXML private TextField whereField;
    @FXML private Button sendButton;

    private final ToggleGroup dayGroup = new ToggleGroup();
    private final ToggleGroup timeGroup = new ToggleGroup();

    /** True while the time chips are being swapped, so the "keep one picked" rule stays out of the way. */
    private boolean rebuilding;

    private Appointment appointment;
    private AppointmentService service;
    private Consumer<VitalsVisitDialog.Choice> onSend;
    private Runnable onClose;

    void configure(Appointment appointment, AppointmentService service,
                   Consumer<VitalsVisitDialog.Choice> onSend, Runnable onClose) {
        this.appointment = appointment;
        this.service = service;
        this.onSend = onSend;
        this.onClose = onClose;

        patientLabel.setText(appointment.getPatientName());
        typeLabel.setText(appointment.getPatientType().getLabel());
        typeLabel.setVisible(appointment.isPriority());
        typeLabel.setManaged(appointment.isPriority());

        LocalDate appointmentDay = LocalDate.parse(appointment.getDate());
        appointmentLabel.setText(appointmentDay.format(LONG_DAY) + " \u00B7 " + appointment.getTime());
        doctorLabel.setText(appointment.getDoctorName());

        hintLabel.setText("The patient is notified right away and sees this in the \"Vitals Check-in\" column. "
                + "It must be at least " + AppointmentService.VITALS_VISIT_LEAD_MINUTES
                + " minutes before the appointment so there is time to forward the request.");

        whereField.textProperty().addListener((observable, was, now) -> refreshSummary());

        keepOneSelected(dayGroup);
        keepOneSelected(timeGroup);
        dayGroup.selectedToggleProperty().addListener((observable, was, now) -> {
            showTimesFor(now == null ? null : (LocalDate) now.getUserData());
        });

        buildDayChips();
        refreshSummary();
    }

    // ------------------------------------------------------------------ chips

    private void buildDayChips() {
        LocalDate first = service.firstVitalsVisitDate(appointment);
        LocalDate last = LocalDate.parse(appointment.getDate());
        int shown = 0;

        for (LocalDate day = first; day != null && !day.isAfter(last) && shown < MAX_DAY_CHIPS;
             day = day.plusDays(1)) {

            if (service.vitalsVisitTimes(appointment, day).isEmpty()) {
                continue;
            }

            String top = day.equals(LocalDate.now()) ? "Today"
                    : day.equals(LocalDate.now().plusDays(1)) ? "Tomorrow"
                    : day.format(WEEKDAY);

            ToggleButton chip = chip(top + "\n" + day.format(MONTH_DAY), dayGroup);
            chip.getStyleClass().add("visit-day-chip");
            chip.setUserData(day);
            dayChips.getChildren().add(chip);
            shown++;
        }

        if (!dayChips.getChildren().isEmpty()) {
            dayGroup.selectToggle((Toggle) dayChips.getChildren().get(0));
        }
    }

    private void showTimesFor(LocalDate day) {
        rebuilding = true;
        timeGroup.getToggles().clear();
        timeChips.getChildren().clear();
        rebuilding = false;

        if (day != null) {
            for (String time : service.vitalsVisitTimes(appointment, day)) {
                ToggleButton chip = chip(time, timeGroup);
                chip.setUserData(time);
                timeChips.getChildren().add(chip);
            }
        }

        if (timeChips.getChildren().isEmpty()) {
            Label none = new Label("No check-in time is left on this day.");
            none.getStyleClass().add("visit-empty");
            timeChips.getChildren().add(none);
        } else {
            timeGroup.selectToggle((Toggle) timeChips.getChildren().get(0));
        }

        refreshSummary();
    }

    private ToggleButton chip(String text, ToggleGroup group) {
        ToggleButton chip = new ToggleButton(text);
        chip.setToggleGroup(group);
        chip.getStyleClass().add("visit-chip");
        return chip;
    }

    /** A plain ToggleGroup lets a second click clear the choice; here one chip always stays picked. */
    private void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((observable, was, now) -> {
            if (rebuilding) {
                return;
            }
            if (now == null && was != null) {
                group.selectToggle(was);
            } else {
                refreshSummary();
            }
        });
    }

    // -------------------------------------------------------------- summaries

    private LocalDate pickedDay() {
        Toggle t = dayGroup.getSelectedToggle();
        return t == null ? null : (LocalDate) t.getUserData();
    }

    private String pickedTime() {
        Toggle t = timeGroup.getSelectedToggle();   // null when the day has no time left
        return t == null ? null : (String) t.getUserData();
    }

    private void refreshSummary() {
        if (checkSummaryLabel == null) {
            return;
        }

        LocalDate day = pickedDay();
        String time = pickedTime();

        checkSummaryLabel.setText(day == null || time == null ? "Pick a day and time"
                : day.format(LONG_DAY) + " \u00B7 " + time);

        String where = whereField.getText() == null ? "" : whereField.getText().trim();
        whereSummaryLabel.setText(where.isEmpty() ? "Clinic nurse station" : where);

        showError(null);
    }

    private void showError(String problem) {
        boolean has = problem != null;
        errorLabel.setText(has ? problem : "");
        errorLabel.setVisible(has);
        errorLabel.setManaged(has);
    }

    // ---------------------------------------------------------------- actions

    @FXML
    private void handleSend() {
        LocalDate day = pickedDay();
        String time = pickedTime();

        String problem = service.vitalsVisitProblem(appointment, day, time);
        if (problem != null) {
            showError(problem);   // stay open so the nurse can fix it
            return;
        }

        onSend.accept(new VitalsVisitDialog.Choice(day, time, whereField.getText()));
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    void focusDefault() {
        sendButton.requestFocus();
    }

    private void close() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
