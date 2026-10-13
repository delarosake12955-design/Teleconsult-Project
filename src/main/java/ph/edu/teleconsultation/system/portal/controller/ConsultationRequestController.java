package ph.edu.teleconsultation.system.portal.controller;

import java.time.LocalDate;
import java.util.ArrayList;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.AccountStatus;
import ph.edu.teleconsultation.system.portal.model.ConsultationRequest;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.service.ConsultationService;
import ph.edu.teleconsultation.system.portal.service.ScheduleService;

/**
 * Patient consultation request page: submit a request and track its status.
 */
public class ConsultationRequestController extends PageController {

    private static final String ALL = "All";

    @FXML private Label totalRequestsLabel;
    @FXML private Label pendingRequestsLabel;
    @FXML private Label approvedRequestsLabel;
    @FXML private Label rejectedRequestsLabel;

    @FXML private Label patientNameLabel;
    @FXML private ComboBox<String> doctorComboBox;
    @FXML private TextArea reasonArea;
    @FXML private DatePicker datePicker;
    @FXML private Label messageLabel;
    @FXML private Button submitRequestButton;

    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private TableView<ConsultationRequest> requestTable;
    @FXML private TableColumn<ConsultationRequest, String> patientColumn;
    @FXML private TableColumn<ConsultationRequest, String> doctorColumn;
    @FXML private TableColumn<ConsultationRequest, String> reasonColumn;
    @FXML private TableColumn<ConsultationRequest, String> dateColumn;
    @FXML private TableColumn<ConsultationRequest, String> statusColumn;

    private final ConsultationService consultationService = new ConsultationService();
    private final ScheduleService scheduleService = new ScheduleService();
    private User currentUser;

    @FXML
    private void initialize() {
        currentUser = LoginController.getLoggedInUser();

        patientColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getPatientName()));
        doctorColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getDoctorName()));
        reasonColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getReason()));
        dateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getDate()));
        statusColumn.setCellValueFactory(
                data -> new SimpleStringProperty(
                        data.getValue().getStatus().replace('_', ' ')));

        requestTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        statusFilterComboBox.getItems().setAll(
                ALL,
                ConsultationController.PENDING,
                ConsultationController.APPROVED,
                ConsultationController.IN_PROGRESS,
                ConsultationController.COMPLETED,
                ConsultationController.REJECTED);
        statusFilterComboBox.getSelectionModel().selectFirst();
        statusFilterComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> loadRequests());

        for (User user : DataStore.users) {
            if (user instanceof Doctor doctor
                    && doctor.getStatus() == AccountStatus.ACTIVE) {
                doctorComboBox.getItems().add(doctor.getFullName());
            }
        }

        datePicker.setValue(LocalDate.now().plusDays(1));
        datePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty
                        || date.isBefore(LocalDate.now())
                        || (doctorComboBox.getValue() != null
                        && !scheduleService.isBookable(doctorComboBox.getValue(), date)));
            }
        });

        boolean patient = currentUser instanceof Patient;
        patientNameLabel.setText(patient
                ? currentUser.getFullName()
                : "Only patients can submit requests");
        submitRequestButton.setDisable(!patient);

        loadRequests();
    }

    @FXML
    private void handleSubmitRequest() {
        if (!(currentUser instanceof Patient patient)) {
            setMessage("Only patients can submit consultation requests.", false);
            return;
        }

        String doctor = doctorComboBox.getValue();
        String reason = reasonArea.getText().trim();
        LocalDate date = datePicker.getValue();

        if (doctor == null) {
            setMessage("Please select a doctor.", false);
            return;
        }

        if (reason.length() < 5) {
            setMessage("Please describe the reason for your consultation.", false);
            return;
        }

        if (date == null || date.isBefore(LocalDate.now())) {
            setMessage("Please choose today or a future date.", false);
            return;
        }

        if (!scheduleService.isBookable(doctor, date)) {
            setMessage(doctor + " is not available on that date. Please choose another date.", false);
            return;
        }

        if (!consultationService.createRequest(new ConsultationRequest(
                DataStore.getNextConsultationId(),
                patient.getFullName(),
                doctor,
                reason,
                date.toString()))) {
            setMessage("You already have an open request with " + doctor
                    + " for that day.", false);
            return;
        }

        reasonArea.clear();
        doctorComboBox.getSelectionModel().clearSelection();
        datePicker.setValue(LocalDate.now().plusDays(1));

        loadRequests();
        setMessage("Request sent to " + doctor + ". You will see its status below.", true);
    }

    @FXML
    private void handleRefresh() {
        loadRequests();
    }

    private void loadRequests() {
        ArrayList<ConsultationRequest> all =
                currentUser instanceof Patient patient
                        ? consultationService.getPatientRequests(patient.getFullName())
                        : new ArrayList<>(consultationService.getAllRequests());

        int pending = 0;
        int approved = 0;
        int rejected = 0;

        for (ConsultationRequest request : all) {
            switch (request.getStatus()) {
                case ConsultationController.PENDING -> pending++;
                case ConsultationController.REJECTED -> rejected++;
                default -> approved++;
            }
        }

        totalRequestsLabel.setText(String.valueOf(all.size()));
        pendingRequestsLabel.setText(String.valueOf(pending));
        approvedRequestsLabel.setText(String.valueOf(approved));
        rejectedRequestsLabel.setText(String.valueOf(rejected));

        String filter = statusFilterComboBox.getValue();
        ArrayList<ConsultationRequest> shown = new ArrayList<>();

        for (ConsultationRequest request : all) {
            if (filter == null || ALL.equals(filter)
                    || request.getStatus().equals(filter)) {
                shown.add(request);
            }
        }

        requestTable.getItems().setAll(shown);
    }

    private void setMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
    }
}
