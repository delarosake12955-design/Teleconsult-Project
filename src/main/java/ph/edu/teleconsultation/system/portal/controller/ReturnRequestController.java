package ph.edu.teleconsultation.system.portal.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;

import ph.edu.teleconsultation.system.portal.model.Appointment;

/** The "Return to patient" card: pick a quick reason or type one, then send the request back. */
public class ReturnRequestController {

    private static final String[] QUICK_REASONS = {
            "Details are incomplete",
            "Please add your symptoms",
            "Wrong doctor or specialty",
            "Please choose another time",
            "Vitals are needed"
    };

    private static final DateTimeFormatter LONG_DAY =
            DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);

    @FXML private Label patientLabel;
    @FXML private Label typeLabel;
    @FXML private Label requestLabel;
    @FXML private Label doctorLabel;
    @FXML private FlowPane reasonChips;
    @FXML private TextArea reasonArea;
    @FXML private Button sendButton;

    private final ToggleGroup reasonGroup = new ToggleGroup();
    private Consumer<String> onReturn;
    private Runnable onClose;

    void configure(List<Appointment> appointments, Consumer<String> onReturn, Runnable onClose) {
        this.onReturn = onReturn;
        this.onClose = onClose;

        if (appointments.size() == 1) {
            Appointment appointment = appointments.get(0);

            patientLabel.setText(appointment.getPatientName());
            typeLabel.setText(appointment.getPatientType().getLabel());
            typeLabel.setVisible(appointment.isPriority());
            typeLabel.setManaged(appointment.isPriority());

            String day = appointment.getDate();
            try {
                day = LocalDate.parse(appointment.getDate()).format(LONG_DAY);
            } catch (RuntimeException ignored) {
                // keep the raw text for an old record
            }
            requestLabel.setText(day + " \u00B7 " + appointment.getTime() + " \u00B7 " + appointment.getCategory());
            doctorLabel.setText(appointment.getDoctorName());
        } else {
            // Several requests share one reason.
            long priority = appointments.stream().filter(Appointment::isPriority).count();
            patientLabel.setText(appointments.size() + " requests selected");
            typeLabel.setText(priority + " priority");
            typeLabel.setVisible(priority > 0);
            typeLabel.setManaged(priority > 0);

            requestLabel.setText("The same reason is sent to each patient");
            String names = appointments.stream().map(Appointment::getPatientName).distinct()
                    .limit(4).collect(java.util.stream.Collectors.joining(", "));
            long distinct = appointments.stream().map(Appointment::getPatientName).distinct().count();
            doctorLabel.setText(names + (distinct > 4 ? " and " + (distinct - 4) + " more" : ""));
        }

        for (String reason : QUICK_REASONS) {
            ToggleButton chip = new ToggleButton(reason);
            chip.setToggleGroup(reasonGroup);
            chip.getStyleClass().addAll("visit-chip", "return-chip");
            chip.setUserData(reason);
            reasonChips.getChildren().add(chip);
        }

        // Choosing a chip fills the box; the nurse can still edit the words.
        reasonGroup.selectedToggleProperty().addListener((observable, was, now) -> {
            if (now != null) {
                reasonArea.setText((String) now.getUserData());
                reasonArea.positionCaret(reasonArea.getText().length());
            }
        });
        // Typing something different un-picks the chip.
        reasonArea.textProperty().addListener((observable, was, now) -> {
            var picked = reasonGroup.getSelectedToggle();
            if (picked != null && !picked.getUserData().equals(now)) {
                reasonGroup.selectToggle(null);
            }
        });
    }

    @FXML
    private void handleSend() {
        String reason = reasonArea.getText() == null ? "" : reasonArea.getText().trim();
        onReturn.accept(reason);
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    void focusDefault() {
        reasonArea.requestFocus();
    }

    private void close() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
