package ph.edu.teleconsultation.system.portal.controller;

import java.time.LocalDate;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.ConsultationRequest;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.MedicalRecord;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.service.ConsultationService;
import ph.edu.teleconsultation.system.portal.service.MedicalRecordService;

/**
 * Consultation workspace. Doctors approve, start and complete consultations;
 * patients and staff follow the progress of the requests they can see.
 */
public class ConsultationController extends PageController {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";

    @FXML private Label totalConsultationsLabel;
    @FXML private Label pendingConsultationsLabel;
    @FXML private Label completedConsultationsLabel;

    @FXML private Label patientNameLabel;
    @FXML private Label doctorNameLabel;
    @FXML private Label consultationDateLabel;
    @FXML private Label reasonLabel;
    @FXML private TextArea consultationNotesArea;
    @FXML private Label messageLabel;

    @FXML private Button approveRequestButton;
    @FXML private Button rejectRequestButton;
    @FXML private Button startButton;
    @FXML private Button completeButton;

    @FXML private VBox consultationHistory;

    private final ConsultationService consultationService = new ConsultationService();
    private final MedicalRecordService recordService = new MedicalRecordService();

    private User currentUser;
    private ConsultationRequest selected;

    @FXML
    private void initialize() {
        currentUser = LoginController.getLoggedInUser();

        boolean doctor = currentUser instanceof Doctor;
        for (Button button : new Button[]{
                approveRequestButton, rejectRequestButton, startButton, completeButton}) {
            if (button != null) {
                button.setVisible(doctor);
                button.setManaged(doctor);
            }
        }

        consultationNotesArea.setEditable(doctor);

        loadConsultations();
    }

    @FXML
    private void handleRefresh() {
        saveDraftNotes();
        loadConsultations();
        setMessage("Consultations refreshed.", true);
    }

    @FXML
    private void handleApproveRequest() {
        if (!ensureSelected()) return;

        if (!PENDING.equals(selected.getStatus())) {
            setMessage("Only pending requests can be approved.", false);
            return;
        }

        consultationService.approveRequest(selected);
        reloadKeepingSelection("Request approved. You can now start the consultation.");
    }

    @FXML
    private void handleRejectRequest() {
        if (!ensureSelected()) return;

        if (!PENDING.equals(selected.getStatus())) {
            setMessage("Only pending requests can be declined.", false);
            return;
        }

        consultationService.rejectRequest(selected);
        reloadKeepingSelection("Request declined.");
    }

    @FXML
    private void handleStartConsultation() {
        if (!ensureSelected()) return;

        String status = selected.getStatus();

        if (!PENDING.equals(status) && !APPROVED.equals(status)) {
            setMessage("Only pending or approved requests can be started.", false);
            return;
        }

        selected.setStatus(IN_PROGRESS);

        new ph.edu.teleconsultation.system.portal.service.NotificationService().notifyByName(
                selected.getPatientName(),
                "Consultation started",
                selected.getDoctorName() + " has started your consultation.",
                ph.edu.teleconsultation.system.portal.model.Notification.Type.INFO);
        reloadKeepingSelection("Consultation started. Add your notes, then complete it.");
        consultationNotesArea.requestFocus();
    }

    @FXML
    private void handleCompleteConsultation() {
        if (!ensureSelected()) return;

        if (!IN_PROGRESS.equals(selected.getStatus())) {
            setMessage("Start the consultation before completing it.", false);
            return;
        }

        String notes = consultationNotesArea.getText().trim();

        if (notes.isEmpty()) {
            setMessage("Enter consultation notes before completing.", false);
            return;
        }

        selected.setNotes(notes);
        selected.setStatus(COMPLETED);

        String firstLine = notes.split("\\R", 2)[0].trim();
        String diagnosis = firstLine.length() > 60
                ? firstLine.substring(0, 57) + "..."
                : firstLine;

        recordService.addRecord(new MedicalRecord(
                DataStore.getNextMedicalRecordId(),
                selected.getPatientName(),
                selected.getDoctorName(),
                LocalDate.now().toString(),
                diagnosis,
                notes,
                new ph.edu.teleconsultation.system.portal.service.AppointmentService().findVitals(
                        selected.getPatientName(), selected.getDoctorName(), selected.getDate())));

        reloadKeepingSelection("Consultation completed and saved to medical records.");
    }

    // ------------------------------------------------------------------ loading

    private ArrayList<ConsultationRequest> visibleRequests() {
        if (currentUser instanceof Doctor doctor) {
            return consultationService.getDoctorRequests(doctor.getFullName());
        }

        if (currentUser instanceof Patient patient) {
            return consultationService.getPatientRequests(patient.getFullName());
        }

        return new ArrayList<>(consultationService.getAllRequests());
    }

    private void loadConsultations() {
        ArrayList<ConsultationRequest> requests = visibleRequests();

        int open = 0;
        int completed = 0;

        for (ConsultationRequest request : requests) {
            String status = request.getStatus();

            if (COMPLETED.equals(status)) {
                completed++;
            } else if (PENDING.equals(status) || APPROVED.equals(status)
                    || IN_PROGRESS.equals(status)) {
                open++;
            }
        }

        totalConsultationsLabel.setText(String.valueOf(requests.size()));
        pendingConsultationsLabel.setText(String.valueOf(open));
        completedConsultationsLabel.setText(String.valueOf(completed));

        consultationHistory.getChildren().clear();

        if (requests.isEmpty()) {
            Label empty = new Label("No consultations yet.");
            empty.getStyleClass().add("empty-label");
            consultationHistory.getChildren().add(empty);
        }

        // Newest first
        for (int i = requests.size() - 1; i >= 0; i--) {
            consultationHistory.getChildren().add(buildHistoryItem(requests.get(i)));
        }

        if (selected != null && !requests.contains(selected)) {
            selected = null;
        }

        showSelected();
    }

    private HBox buildHistoryItem(ConsultationRequest request) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.getStyleClass().add("history-item");

        if (request == selected) {
            item.getStyleClass().add("history-item-selected");
        }

        VBox left = new VBox(5);
        HBox.setHgrow(left, Priority.ALWAYS);

        Label patient = new Label(request.getPatientName());
        patient.getStyleClass().add("history-name");

        Label doctor = new Label(request.getDoctorName());
        doctor.getStyleClass().add("history-doctor");

        left.getChildren().addAll(patient, doctor);

        VBox right = new VBox(5);
        right.setAlignment(Pos.CENTER_RIGHT);

        Label date = new Label(request.getDate());
        date.getStyleClass().add("history-date");

        Label status = new Label(request.getStatus().replace('_', ' '));
        status.getStyleClass().addAll("history-status", statusStyle(request.getStatus()));

        right.getChildren().addAll(date, status);

        item.getChildren().addAll(left, right);
        item.setOnMouseClicked(event -> {
            saveDraftNotes();
            selected = request;
            setMessage("", true);
            loadConsultations();
        });

        return item;
    }

    private void showSelected() {
        if (selected == null) {
            patientNameLabel.setText("No patient selected");
            doctorNameLabel.setText("No doctor selected");
            consultationDateLabel.setText("No date");
            reasonLabel.setText("Select a consultation from the list below.");
            consultationNotesArea.clear();
            updateButtons();
            return;
        }

        patientNameLabel.setText(selected.getPatientName());
        doctorNameLabel.setText(selected.getDoctorName());
        consultationDateLabel.setText(selected.getDate());
        reasonLabel.setText(selected.getReason());
        consultationNotesArea.setText(selected.getNotes());
        updateButtons();
    }

    private void updateButtons() {
        String status = selected == null ? "" : selected.getStatus();

        boolean pending = PENDING.equals(status);

        if (approveRequestButton != null) approveRequestButton.setDisable(!pending);
        if (rejectRequestButton != null) rejectRequestButton.setDisable(!pending);
        if (startButton != null) {
            startButton.setDisable(!(pending || APPROVED.equals(status)));
        }
        if (completeButton != null) {
            completeButton.setDisable(!IN_PROGRESS.equals(status));
        }

        boolean canWrite = currentUser instanceof Doctor && IN_PROGRESS.equals(status);
        consultationNotesArea.setDisable(!canWrite && selected == null);
        consultationNotesArea.setEditable(canWrite);
    }

    // ------------------------------------------------------------------ helpers

    /** Keeps notes typed for an in-progress consultation when switching items. */
    private void saveDraftNotes() {
        if (selected != null && currentUser instanceof Doctor
                && IN_PROGRESS.equals(selected.getStatus())) {
            selected.setNotes(consultationNotesArea.getText());
        }
    }

    private boolean ensureSelected() {
        if (selected == null) {
            setMessage("Select a consultation from the list first.", false);
            return false;
        }
        return true;
    }

    private void reloadKeepingSelection(String message) {
        loadConsultations();
        setMessage(message, true);
    }

    static String statusStyle(String status) {
        return switch (status) {
            case COMPLETED -> "status-completed";
            case APPROVED, IN_PROGRESS -> "status-approved";
            case REJECTED -> "status-rejected";
            default -> "status-pending";
        };
    }

    private void setMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        if (!text.isEmpty()) {
            messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
        }
    }
}
