package ph.edu.teleconsultation.system.portal.controller;

import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import ph.edu.teleconsultation.system.portal.model.VitalSigns;

/** Small form a nurse uses to enter vital signs (validated ranges). */
public final class VitalsDialog {

    private VitalsDialog() {
    }

    public static Optional<VitalSigns> show(String patientName, VitalSigns existing) {

        TextField temperature = new TextField(existing == null ? "" : String.valueOf(existing.getTemperature()));
        TextField heartRate = new TextField(existing == null ? "" : String.valueOf(existing.getHeartRate()));
        TextField systolic = new TextField(existing == null ? "" : String.valueOf(existing.getSystolic()));
        TextField diastolic = new TextField(existing == null ? "" : String.valueOf(existing.getDiastolic()));
        TextField respiratory = new TextField(existing == null ? "" : String.valueOf(existing.getRespiratoryRate()));
        Label error = new Label();
        error.setStyle("-fx-text-fill: #c0392b;");
        temperature.setPromptText("e.g. 36.8");
        heartRate.setPromptText("e.g. 72");
        systolic.setPromptText("e.g. 120");
        diastolic.setPromptText("e.g. 80");
        respiratory.setPromptText("e.g. 16");

        GridPane grid = new GridPane();
        DialogTheme.styleForm(grid);
        grid.addRow(0, new Label("Temperature (\u00B0C)"), temperature);
        grid.addRow(1, new Label("Heart rate (bpm)"), heartRate);
        grid.addRow(2, new Label("Systolic (mmHg)"), systolic);
        grid.addRow(3, new Label("Diastolic (mmHg)"), diastolic);
        grid.addRow(4, new Label("Respiratory rate (/min)"), respiratory);
        grid.add(error, 0, 5, 2, 1);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

        Dialog<VitalSigns> dialog = new Dialog<>();
        dialog.setTitle("Vital Signs");
        dialog.setHeaderText("Vital signs for " + patientName);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        DialogTheme.apply(dialog);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (parse(temperature, heartRate, systolic, diastolic, respiratory, error) == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == save
                ? parse(temperature, heartRate, systolic, diastolic, respiratory, error)
                : null);

        return dialog.showAndWait().map(result -> {
            ph.edu.teleconsultation.system.portal.model.User user = LoginController.getLoggedInUser();
            if (user != null) {
                result.stamp(user.getFullName());
            }
            return result;
        });
    }

    private static VitalSigns parse(TextField temperature, TextField heartRate,
                                    TextField systolic, TextField diastolic,
                                    TextField respiratory, Label error) {
        try {
            double temp = Double.parseDouble(temperature.getText().trim());
            int hr = Integer.parseInt(heartRate.getText().trim());
            int sys = Integer.parseInt(systolic.getText().trim());
            int dia = Integer.parseInt(diastolic.getText().trim());
            int rr = Integer.parseInt(respiratory.getText().trim());

            String problem = VitalSigns.check(temp, hr, sys, dia, rr);

            if (problem != null) {
                error.setText(problem);
            } else {
                error.setText("");
                return new VitalSigns(temp, hr, sys, dia, rr);
            }
        } catch (NumberFormatException e) {
            error.setText("Fill in every field with numbers only.");
        }

        return null;
    }
}
