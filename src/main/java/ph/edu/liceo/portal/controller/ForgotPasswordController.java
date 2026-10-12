package ph.edu.liceo.portal.controller;

import java.io.IOException;
import java.time.LocalDate;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import ph.edu.liceo.portal.MainApp;
import ph.edu.liceo.portal.service.PasswordService;

/**
 * Forgot password: pick your role, confirm who you are with details from registration,
 * choose a new password. Enter moves to the next field; Enter in the last field resets.
 */
public class ForgotPasswordController {

    @FXML private Button patientButton;
    @FXML private Button doctorButton;
    @FXML private Button nurseButton;
    @FXML private Button secretaryButton;

    @FXML private TextField emailField;
    @FXML private TextField fullNameField;
    @FXML private Label detailLabel;
    @FXML private DatePicker dobPicker;
    @FXML private TextField detailField;

    @FXML private TextField patientIdField;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newVisibleField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmVisibleField;
    @FXML private CheckBox showPasswordCheckBox;

    @FXML private Label messageLabel;
    @FXML private Button resetButton;

    private final PasswordService passwordService = new PasswordService();

    private String selectedRole = "Patient";

    @FXML
    private void initialize() {
        PasswordToggle.link(newPasswordField, newVisibleField);
        PasswordToggle.link(confirmPasswordField, confirmVisibleField);

        // forgot-password-patient / -doctor / -nurse / -secretary.fxml set the role on the
        // reset button. The older all-in-one page has role buttons instead.
        if (resetButton.getUserData() instanceof String role && !role.isBlank()) {
            selectedRole = role;
        }

        // Same date entry as the registration page: type it or pick it.
        if (dobPicker != null) {
            dobPicker.setEditable(true);
        }

        updateRoleButtons();
        updateDetailField();
    }

    // ------------------------------------------------------------------ role

    @FXML
    private void selectPatient() {
        selectRole("Patient");
    }

    @FXML
    private void selectDoctor() {
        selectRole("Doctor");
    }

    @FXML
    private void selectNurse() {
        selectRole("Nurse");
    }

    @FXML
    private void selectSecretary() {
        selectRole("Secretary");
    }

    private void selectRole(String role) {
        selectedRole = role;
        messageLabel.setText("");
        updateRoleButtons();
        updateDetailField();
    }

    private void updateRoleButtons() {
        mark(patientButton, "Patient");
        mark(doctorButton, "Doctor");
        mark(nurseButton, "Nurse");
        mark(secretaryButton, "Secretary");
    }

    private void mark(Button button, String role) {

        if (button == null) {
            return;
        }

        button.getStyleClass().remove("role-selected");

        if (selectedRole.equals(role)) {
            button.getStyleClass().add("role-selected");
        }
    }

    /** The identity detail depends on the role (see PasswordService). */
    private void updateDetailField() {

        // Role pages only contain the field their role needs, so skip what is missing.
        if (dobPicker == null || detailField == null || detailLabel == null) {
            return;
        }

        boolean patient = selectedRole.equals("Patient");

        dobPicker.setVisible(patient);
        dobPicker.setManaged(patient);

        detailField.setVisible(!patient);
        detailField.setManaged(!patient);
        detailField.clear();

        switch (selectedRole) {
            case "Doctor", "Nurse" -> {
                detailLabel.setText("License Number");
                detailField.setPromptText("Enter your license number");
            }
            case "Secretary" -> {
                detailLabel.setText("Department");
                detailField.setPromptText("Enter your department");
            }
            default -> detailLabel.setText("Date of Birth");
        }
    }

    // ----------------------------------------------------------------- Enter key

    @FXML
    private void handleEmailEnter() {
        fullNameField.requestFocus();
    }

    @FXML
    private void handleNameEnter() {
        (selectedRole.equals("Patient") ? dobPicker : detailField).requestFocus();
    }

    @FXML
    private void handleDetailEnter() {
        boolean show = showPasswordCheckBox.isSelected();

        if (patientIdField != null) {
            patientIdField.requestFocus();
            return;
        }

        (show ? newVisibleField : newPasswordField).requestFocus();
    }

    @FXML
    private void handlePatientIdEnter() {
        (showPasswordCheckBox.isSelected() ? newVisibleField : newPasswordField).requestFocus();
    }

    @FXML
    private void handleNewEnter() {
        (showPasswordCheckBox.isSelected() ? confirmVisibleField : confirmPasswordField).requestFocus();
    }

    // ---------------------------------------------------------------- actions

    @FXML
    private void handleShowPassword() {
        boolean show = showPasswordCheckBox.isSelected();

        PasswordToggle.setShown(show, newPasswordField, newVisibleField);
        PasswordToggle.setShown(show, confirmPasswordField, confirmVisibleField);
    }

    @FXML
    private void handleReset() {

        PasswordService.Result result = passwordService.resetPassword(
                selectedRole,
                emailField.getText(),
                fullNameField.getText(),
                detailText(),
                patientIdField == null ? null : patientIdField.getText(),
                newPasswordField.getText(),
                confirmPasswordField.getText());

        if (!result.isOk()) {
            messageLabel.setText(result.getMessage());
            return;
        }

        clearForm();

        MessagePopup.show(
                resetButton.getScene(),
                "Password Updated",
                result.getMessage(),
                "Back to Login",
                this::openLogin);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        openLogin();
    }

    /** Patients: the picked or typed date; everyone else: the text field. */
    private String detailText() {

        if (!selectedRole.equals("Patient")) {
            return detailField == null ? "" : detailField.getText();
        }

        if (dobPicker == null) {
            return "";
        }

        LocalDate picked = dobPicker.getValue();

        if (picked != null) {
            return picked.toString();
        }

        String typed = dobPicker.getEditor().getText();

        return typed == null ? "" : typed.trim();
    }

    private void clearForm() {
        emailField.clear();
        fullNameField.clear();
        if (detailField != null) {
            detailField.clear();
        }
        if (dobPicker != null) {
            dobPicker.setValue(null);
            dobPicker.getEditor().clear();
        }
        if (patientIdField != null) {
            patientIdField.clear();
        }
        newPasswordField.clear();
        confirmPasswordField.clear();
        messageLabel.setText("");
    }

    private void openLogin() {

        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/liceo/portal/login-" + selectedRole.toLowerCase() + ".fxml"));

            MainApp.showRoot(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
