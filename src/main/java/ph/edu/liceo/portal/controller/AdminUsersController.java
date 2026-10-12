package ph.edu.liceo.portal.controller;

import java.util.ArrayList;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.AccountStatus;
import ph.edu.liceo.portal.model.Admin;
import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.Nurse;
import ph.edu.liceo.portal.model.Patient;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.service.UserService;

/**
 * Administrator user management: search, filter, activate, deactivate, delete.
 */
public class AdminUsersController extends PageController {

    private static final String ALL_ROLES = "All roles";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterComboBox;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> patientIdColumn;
    @FXML private TableColumn<User, String> licenseColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;

    @FXML private Label messageLabel;

    private final UserService userService = new UserService();

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getFullName()));
        emailColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getEmail()));
        patientIdColumn.setCellValueFactory(
                data -> new SimpleStringProperty(patientIdOf(data.getValue())));
        licenseColumn.setCellValueFactory(
                data -> new SimpleStringProperty(licenseOf(data.getValue())));
        roleColumn.setCellValueFactory(
                data -> new SimpleStringProperty(AdminController.roleName(data.getValue())));
        statusColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getStatus().name()));

        userTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        roleFilterComboBox.getItems().setAll(
                ALL_ROLES, "Patient", "Doctor", "Nurse", "Secretary", "Admin");
        roleFilterComboBox.getSelectionModel().selectFirst();
        roleFilterComboBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> loadUsers());

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> loadUsers());

        loadUsers();
    }

    @FXML
    private void handleSearch() {
        loadUsers();
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        roleFilterComboBox.getSelectionModel().selectFirst();
        loadUsers();
        setMessage("User list refreshed.", true);
    }

    @FXML
    private void handleActivate() {
        User user = selectedUser();
        if (user == null) return;

        userService.approveUser(user);
        loadUsers();
        setMessage(user.getFullName() + " is now active.", true);
    }

    @FXML
    private void handleDeactivate() {
        User user = selectedUser();
        if (user == null) return;

        if (isProtected(user)) {
            setMessage("You cannot deactivate your own or another administrator account.", false);
            return;
        }

        userService.rejectUser(user);
        loadUsers();
        setMessage(user.getFullName() + " was deactivated.", true);
    }

    @FXML
    private void handleDelete() {
        User user = selectedUser();
        if (user == null) return;

        if (isProtected(user)) {
            setMessage("You cannot delete your own or another administrator account.", false);
            return;
        }

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete the account of " + user.getFullName() + "? This cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Delete account");

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        userService.deleteUser(user);
        loadUsers();
        setMessage("Account deleted.", true);
    }

    /** Patients only; "-" for everyone else. */
    private static String patientIdOf(User user) {
        return user instanceof Patient patient ? patient.getPatientId() : "-";
    }

    /** Doctors and nurses only; "-" for everyone else or when none was given. */
    private static String licenseOf(User user) {
        String license = null;

        if (user instanceof Doctor doctor) {
            license = doctor.getLicenseNumber();
        } else if (user instanceof Nurse nurse) {
            license = nurse.getLicenseNumber();
        }

        return license == null || license.isBlank() ? "-" : license.trim();
    }

    private boolean isProtected(User user) {
        return user == LoginController.getLoggedInUser() || user instanceof Admin;
    }

    private User selectedUser() {
        User user = userTable.getSelectionModel().getSelectedItem();

        if (user == null) {
            setMessage("Select a user first.", false);
        }

        return user;
    }

    private void loadUsers() {
        String query = searchField.getText() == null
                ? "" : searchField.getText().trim().toLowerCase();
        String role = roleFilterComboBox.getValue();

        ArrayList<User> shown = new ArrayList<>();
        int active = 0;

        for (User user : DataStore.users) {
            if (user.getStatus() == AccountStatus.ACTIVE) {
                active++;
            }

            boolean roleMatch = role == null || ALL_ROLES.equals(role)
                    || AdminController.roleName(user).equals(role);
            boolean textMatch = query.isEmpty()
                    || user.getFullName().toLowerCase().contains(query)
                    || user.getEmail().toLowerCase().contains(query)
                    || patientIdOf(user).toLowerCase().contains(query)
                    || licenseOf(user).toLowerCase().contains(query);

            if (roleMatch && textMatch) {
                shown.add(user);
            }
        }

        userTable.getItems().setAll(shown);
        totalUsersLabel.setText(String.valueOf(DataStore.users.size()));
        activeUsersLabel.setText(String.valueOf(active));
    }

    private void setMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
    }
}
