package ph.edu.liceo.portal.controller;

import java.util.ArrayList;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.MedicalRecord;
import ph.edu.liceo.portal.model.Nurse;
import ph.edu.liceo.portal.model.Patient;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.model.VitalSigns;
import ph.edu.liceo.portal.service.MedicalRecordService;

/**
 * Medical records for every role. Patients only see their own records,
 * doctors see the records they authored, all other staff see everything.
 */
public class MedicalRecordController extends PageController {

    @FXML private Label totalRecordsLabel;
    @FXML private Label latestDiagnosisLabel;
    @FXML private Label latestDateLabel;

    @FXML private TableView<MedicalRecord> medicalRecordTable;
    @FXML private TableColumn<MedicalRecord, String> patientColumn;
    @FXML private TableColumn<MedicalRecord, String> doctorColumn;
    @FXML private TableColumn<MedicalRecord, String> dateColumn;
    @FXML private TableColumn<MedicalRecord, String> diagnosisColumn;
    @FXML private TableColumn<MedicalRecord, String> treatmentColumn;

    @FXML private Label selectedDiagnosisLabel;
    @FXML private Label selectedTreatmentLabel;
    @FXML private Label vitalEmptyLabel;
    @FXML private VBox vitalContent;
    @FXML private HBox vitalActions;
    @FXML private Label vitalMetaLabel;
    @FXML private Label vitalTemperatureLabel;
    @FXML private Label vitalTemperatureStatus;
    @FXML private Label vitalHeartRateLabel;
    @FXML private Label vitalHeartRateStatus;
    @FXML private Label vitalBloodPressureLabel;
    @FXML private Label vitalBloodPressureStatus;
    @FXML private Label vitalRespiratoryLabel;
    @FXML private Label vitalRespiratoryStatus;

    private final MedicalRecordService recordService = new MedicalRecordService();

    @FXML
    private void initialize() {
        patientColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getPatientName()));
        doctorColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getDoctorName()));
        dateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getDate()));
        diagnosisColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getDiagnosis()));
        treatmentColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getTreatment()));

        medicalRecordTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        medicalRecordTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showDetails(newValue));

        setupVitalsEditor();

        loadRecords();
    }

    /** Nurses record vital signs; other roles only view them. */
    private void setupVitalsEditor() {
        if (!(LoginController.getLoggedInUser() instanceof Nurse)) {
            return;
        }

        Button button = new Button("Record Vital Signs");
        button.getStyleClass().add("secondary-button");
        button.setOnAction(event -> recordVitals());
        vitalActions.getChildren().add(button);
    }

    private void recordVitals() {
        MedicalRecord record = medicalRecordTable.getSelectionModel().getSelectedItem();

        if (record == null) {
            Alert notice = new Alert(Alert.AlertType.INFORMATION,
                    "Select a medical record first.");
            DialogTheme.apply(notice);
            notice.showAndWait();
            return;
        }

        VitalSigns existing = record.getVitalSigns();

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
        grid.addRow(0, new Label("Temperature (°C)"), temperature);
        grid.addRow(1, new Label("Heart rate (bpm)"), heartRate);
        grid.addRow(2, new Label("Systolic (mmHg)"), systolic);
        grid.addRow(3, new Label("Diastolic (mmHg)"), diastolic);
        grid.addRow(4, new Label("Respiratory rate (/min)"), respiratory);
        grid.add(error, 0, 5, 2, 1);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

        Dialog<VitalSigns> dialog = new Dialog<>();
        dialog.setTitle("Vital Signs");
        dialog.setHeaderText("Vital signs for " + record.getPatientName());
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        DialogTheme.apply(dialog);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            VitalSigns parsed = parseVitals(temperature, heartRate, systolic, diastolic, respiratory, error);
            if (parsed == null) {
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == save
                ? parseVitals(temperature, heartRate, systolic, diastolic, respiratory, error)
                : null);

        dialog.showAndWait().ifPresent(vitals -> {
            if (LoginController.getLoggedInUser() != null) {
                vitals.stamp(LoginController.getLoggedInUser().getFullName());
            }
            record.setVitalSigns(vitals);

            ph.edu.liceo.portal.service.NotificationService notifier =
                    new ph.edu.liceo.portal.service.NotificationService();

            notifier.notifyByName(record.getPatientName(),
                    "Vital signs recorded",
                    "A nurse recorded your vital signs.",
                    ph.edu.liceo.portal.model.Notification.Type.SUCCESS);

            notifier.notifyByName(record.getDoctorName(),
                    "Vital signs recorded",
                    "Vitals for " + record.getPatientName() + " are now on the record.",
                    ph.edu.liceo.portal.model.Notification.Type.INFO);
            medicalRecordTable.refresh();
            showDetails(record);
        });
    }

    private VitalSigns parseVitals(TextField temperature, TextField heartRate,
                                   TextField systolic, TextField diastolic,
                                   TextField respiratory, Label error) {
        try {
            double temp = Double.parseDouble(temperature.getText().trim());
            int hr = Integer.parseInt(heartRate.getText().trim());
            int sys = Integer.parseInt(systolic.getText().trim());
            int dia = Integer.parseInt(diastolic.getText().trim());
            int rr = Integer.parseInt(respiratory.getText().trim());

            if (temp < 30 || temp > 45) {
                error.setText("Temperature must be between 30 and 45 °C.");
            } else if (hr < 20 || hr > 250) {
                error.setText("Heart rate must be between 20 and 250 bpm.");
            } else if (sys < 50 || sys > 260 || dia < 30 || dia > 160 || sys <= dia) {
                error.setText("Enter a realistic blood pressure (systolic above diastolic).");
            } else if (rr < 4 || rr > 60) {
                error.setText("Respiratory rate must be between 4 and 60 per minute.");
            } else {
                error.setText("");
                return new VitalSigns(temp, hr, sys, dia, rr);
            }
        } catch (NumberFormatException e) {
            error.setText("Fill in every field with numbers only.");
        }

        return null;
    }

    @FXML
    private void handleRefresh() {
        loadRecords();
    }

    private void loadRecords() {
        User user = LoginController.getLoggedInUser();
        ArrayList<MedicalRecord> records;

        if (user instanceof Patient patient) {
            records = recordService.getPatientRecords(patient.getFullName());
        } else if (user instanceof Doctor doctor) {
            records = recordService.getDoctorRecords(doctor.getFullName());
        } else {
            records = new ArrayList<>(recordService.getAllRecords());
        }

        medicalRecordTable.getItems().setAll(records);
        totalRecordsLabel.setText(String.valueOf(records.size()));

        if (records.isEmpty()) {
            latestDiagnosisLabel.setText("None yet");
            latestDateLabel.setText("—");
            showDetails(null);
            return;
        }

        MedicalRecord latest = records.get(0);
        for (MedicalRecord record : records) {
            if (record.getDate().compareTo(latest.getDate()) >= 0) {
                latest = record;
            }
        }

        latestDiagnosisLabel.setText(latest.getDiagnosis());
        latestDateLabel.setText(latest.getDate());

        medicalRecordTable.getSelectionModel().select(latest);
    }

    private void showDetails(MedicalRecord record) {
        if (record == null) {
            selectedDiagnosisLabel.setText("Select a record");
            selectedTreatmentLabel.setText("—");
            showVitalsMessage("Select a medical record to view vital signs.");
            return;
        }

        selectedDiagnosisLabel.setText(record.getDiagnosis());
        selectedTreatmentLabel.setText(record.getTreatment());

        VitalSigns vitals = record.getVitalSigns();

        if (vitals == null) {
            showVitalsMessage("No vital signs were recorded for this visit.");
            return;
        }

        vitalTemperatureLabel.setText(String.valueOf(vitals.getTemperature()));
        vitalHeartRateLabel.setText(String.valueOf(vitals.getHeartRate()));
        vitalBloodPressureLabel.setText(vitals.getSystolic() + "/" + vitals.getDiastolic());
        vitalRespiratoryLabel.setText(String.valueOf(vitals.getRespiratoryRate()));

        setStatus(vitalTemperatureStatus, rangeOf(vitals.getTemperature(), 36.1, 37.5));
        setStatus(vitalHeartRateStatus, rangeOf(vitals.getHeartRate(), 60, 100));
        setStatus(vitalBloodPressureStatus, vitals.getSystolic() > 129 || vitals.getDiastolic() > 84 ? 1
                : vitals.getSystolic() < 90 || vitals.getDiastolic() < 60 ? -1 : 0);
        setStatus(vitalRespiratoryStatus, rangeOf(vitals.getRespiratoryRate(), 12, 20));

        if (vitals.getRecordedBy() != null) {
            String when = vitals.getRecordedAt() == null ? "" : "  \u00B7  " + vitals.getRecordedAt();
            vitalMetaLabel.setText("Recorded by " + vitals.getRecordedBy() + when);
            vitalMetaLabel.setVisible(true);
            vitalMetaLabel.setManaged(true);
        } else {
            vitalMetaLabel.setVisible(false);
            vitalMetaLabel.setManaged(false);
        }

        vitalEmptyLabel.setVisible(false);
        vitalEmptyLabel.setManaged(false);
        vitalContent.setVisible(true);
        vitalContent.setManaged(true);
    }

    /** -1 below, 0 inside, 1 above the typical adult range. */
    private static int rangeOf(double value, double low, double high) {
        return value < low ? -1 : value > high ? 1 : 0;
    }

    private static void setStatus(Label label, int position) {
        label.setText(position == 0 ? "Within typical range"
                : position > 0 ? "Above typical range" : "Below typical range");
        label.getStyleClass().removeAll("vital-status-ok", "vital-status-warn");
        label.getStyleClass().add(position == 0 ? "vital-status-ok" : "vital-status-warn");
    }

    /** Shows a short message in place of the vital-sign tiles. */
    private void showVitalsMessage(String message) {
        vitalEmptyLabel.setText(message);
        vitalEmptyLabel.setVisible(true);
        vitalEmptyLabel.setManaged(true);
        vitalContent.setVisible(false);
        vitalContent.setManaged(false);
    }
}
