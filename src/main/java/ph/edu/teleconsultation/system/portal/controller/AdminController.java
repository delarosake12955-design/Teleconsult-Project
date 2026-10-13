package ph.edu.teleconsultation.system.portal.controller;

import java.util.ArrayList;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.AccountStatus;
import ph.edu.teleconsultation.system.portal.model.Admin;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.service.UserService;

/**
 * Account verification page shared by administrators and secretaries.
 */
public class AdminController extends PageController {

    private static final String ALL = "All";

    @FXML private Label pendingCountLabel;
    @FXML private Label approvedCountLabel;
    @FXML private Label rejectedCountLabel;

    @FXML private ComboBox<String> statusFilterComboBox;

    @FXML private TableView<User> verificationTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;

    @FXML private Label selectedNameLabel;
    @FXML private Label selectedEmailLabel;
    @FXML private Label selectedRoleLabel;
    @FXML private Label selectedLicenseLabel;
    @FXML private Label selectedDetailTitleLabel;
    @FXML private Label selectedDetailLabel;
    @FXML private Label selectedStatusLabel;
    @FXML private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getFullName()));
        emailColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getEmail()));
        roleColumn.setCellValueFactory(
                data -> new SimpleStringProperty(roleName(data.getValue())));
        statusColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        verificationTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        statusFilterComboBox.getItems().setAll(
                ALL,
                AccountStatus.PENDING.name(),
                AccountStatus.ACTIVE.name(),
                AccountStatus.REJECTED.name());
        statusFilterComboBox.getSelectionModel().select(AccountStatus.PENDING.name());
        statusFilterComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> loadRequests());

        verificationTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showSelected(newValue));

        loadRequests();
    }

    @FXML
    private void handleRefresh() {
        loadRequests();
        setMessage("List refreshed.", true);
    }

    @FXML
    private void handleApprove() {
        User user = verificationTable.getSelectionModel().getSelectedItem();

        if (user == null) {
            setMessage("Select an account first.", false);
            return;
        }

        if (LoginController.getLoggedInUser() instanceof Secretary
                && user instanceof Secretary) {
            setMessage("Secretary accounts must be verified by an administrator.", false);
            return;
        }

        String license = licenseOf(user);

        userService.approveUser(user);
        loadRequests();
        setMessage(user.getFullName()
                + (license.startsWith("Not") ? "" : " (License " + license + ")")
                + " was approved and can now sign in.", true);
    }

    @FXML
    private void handleReject() {
        User user = verificationTable.getSelectionModel().getSelectedItem();

        if (user == null) {
            setMessage("Select an account first.", false);
            return;
        }

        if (LoginController.getLoggedInUser() instanceof Secretary
                && user instanceof Secretary) {
            setMessage("Secretary accounts must be verified by an administrator.", false);
            return;
        }

        userService.rejectUser(user);
        loadRequests();
        setMessage(user.getFullName() + " was rejected.", true);
    }

    private void loadRequests() {
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        String filter = statusFilterComboBox.getValue();
        ArrayList<User> shown = new ArrayList<>();

        for (User user : DataStore.users) {
            // Administrators and patients never need verification.
            if (user instanceof Admin || user instanceof Patient) {
                continue;
            }

            switch (user.getStatus()) {
                case PENDING -> pending++;
                case ACTIVE -> approved++;
                case REJECTED -> rejected++;
            }

            if (filter == null || ALL.equals(filter)
                    || user.getStatus().name().equals(filter)) {
                shown.add(user);
            }
        }

        pendingCountLabel.setText(String.valueOf(pending));
        approvedCountLabel.setText(String.valueOf(approved));
        rejectedCountLabel.setText(String.valueOf(rejected));

        verificationTable.getItems().setAll(shown);
        showSelected(null);
    }

    private void showSelected(User user) {
        if (user == null) {
            selectedNameLabel.setText("No account selected");
            selectedEmailLabel.setText("—");
            selectedRoleLabel.setText("—");
            selectedLicenseLabel.setText("—");
            selectedLicenseLabel.getStyleClass().remove("license-missing");
            selectedDetailTitleLabel.setText("SPECIALTY / DEPARTMENT");
            selectedDetailLabel.setText("—");
            selectedStatusLabel.setText("—");
            return;
        }

        selectedNameLabel.setText(user.getFullName());
        selectedEmailLabel.setText(user.getEmail());
        selectedRoleLabel.setText(roleName(user));
        selectedStatusLabel.setText(user.getStatus().name());

        // The license number is what the reviewer checks before approving.
        String license = licenseOf(user);
        selectedLicenseLabel.setText(license);
        selectedLicenseLabel.getStyleClass().remove("license-missing");

        if (license.equals("Not provided")) {
            selectedLicenseLabel.getStyleClass().add("license-missing");
        }

        if (user instanceof Doctor doctor) {
            selectedDetailTitleLabel.setText("SPECIALTY");
            selectedDetailLabel.setText(valueOrDash(doctor.getSpecialty()));
        } else if (user instanceof Nurse nurse) {
            selectedDetailTitleLabel.setText("DEPARTMENT");
            selectedDetailLabel.setText(valueOrDash(nurse.getDepartment()));
        } else if (user instanceof Secretary secretary) {
            selectedDetailTitleLabel.setText("DEPARTMENT");
            selectedDetailLabel.setText(valueOrDash(secretary.getDepartment()));
        } else {
            selectedDetailTitleLabel.setText("DETAILS");
            selectedDetailLabel.setText("—");
        }
    }

    /** License number for doctors and nurses; secretaries do not need one. */
    private static String licenseOf(User user) {
        String license;

        if (user instanceof Doctor doctor) {
            license = doctor.getLicenseNumber();
        } else if (user instanceof Nurse nurse) {
            license = nurse.getLicenseNumber();
        } else {
            return "Not required";
        }

        return license == null || license.isBlank() ? "Not provided" : license.trim();
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

    static String roleName(User user) {
        if (user instanceof Admin) return "Admin";
        if (user instanceof Doctor) return "Doctor";
        if (user instanceof Nurse) return "Nurse";
        if (user instanceof Secretary) return "Secretary";
        if (user instanceof Patient) return "Patient";
        return "User";
    }

    private void setMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
    }
}
