package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.service.UserService;

public class PatientRegistrationController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField visibleConfirmPasswordField;

    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private DatePicker dateOfBirthPicker;

    @FXML
    private TextField phoneNumberField;

    @FXML
    private TextField addressField;

    @FXML
    private CheckBox pwdCheckBox;

    @FXML
    private TextField pwdIdField;

    @FXML
    private Label messageLabel;

    private UserService userService = new UserService();

    @FXML
    private void initialize() {

        PasswordToggle.link(passwordField, visiblePasswordField);
        PasswordToggle.link(confirmPasswordField, visibleConfirmPasswordField);

        // The PWD ID box only appears when the PWD box is ticked.
        pwdCheckBox.selectedProperty().addListener((observable, was, now) -> {
            pwdIdField.setVisible(now);
            pwdIdField.setManaged(now);
            if (!now) {
                pwdIdField.clear();
            }
        });

        dateOfBirthPicker.setEditable(true);
        dateOfBirthPicker.setPromptText("MM/DD/YYYY  or pick from the calendar");

        // A birth date cannot be in the future (or absurdly old): grey those days out.
        dateOfBirthPicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {

            @Override
            public void updateItem(java.time.LocalDate date, boolean empty) {

                super.updateItem(date, empty);

                setDisable(empty
                        || date.isAfter(java.time.LocalDate.now())
                        || date.getYear() < 1900);
            }
        });
    }

    @FXML
    private void handleShowPassword() {

        boolean show = showPasswordCheckBox.isSelected();

        PasswordToggle.setShown(show, passwordField, visiblePasswordField);
        PasswordToggle.setShown(show, confirmPasswordField, visibleConfirmPasswordField);
    }

    @FXML
    private void handleRegister(ActionEvent event) {

        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        String dateOfBirth = "";

        if (dateOfBirthPicker.getValue() != null) {
            dateOfBirth = dateOfBirthPicker.getValue().toString();
        }

        String phoneNumber = phoneNumberField.getText().trim();
        String address = addressField.getText().trim();

        if (fullName.isEmpty()
                || email.isEmpty()
                || password.isEmpty()
                || dateOfBirth.isEmpty()
                || phoneNumber.isEmpty()
                || address.isEmpty()) {

            messageLabel.setText(
                    "Please complete all fields."
            );

            return;
        }

        if (!password.equals(confirmPasswordField.getText())) {

            messageLabel.setText(
                    "Passwords do not match. Please re-enter them."
            );

            return;
        }

        java.time.LocalDate birthDate = dateOfBirthPicker.getValue();

        if (birthDate.isAfter(java.time.LocalDate.now()) || birthDate.getYear() < 1900) {

            messageLabel.setText(
                    "Please enter a valid date of birth (not in the future)."
            );

            return;
        }

        if (!email.toLowerCase().endsWith("@gmail.com")) {

            messageLabel.setText(
                    "Please use a valid Gmail address."
            );

            return;
        }

        if (!isStrongPassword(password)) {

            messageLabel.setText(
                    "Password must be 8+ characters with uppercase, lowercase, number, and special character."
            );

            return;
        }

        boolean pwd = pwdCheckBox.isSelected();
        String pwdId = pwdIdField.getText().trim();

        if (pwd && !isValidPwdId(pwdId)) {
            messageLabel.setText(
                    "Enter your PWD ID number, for example PWD-2026-0001 (letters, numbers and dashes, 4 to 30 characters)."
            );
            return;
        }

        if (userService.nameExists(fullName)) {

            messageLabel.setText(
                    "An account with this full name already exists. Add a middle name or initial."
            );

            return;
        }

        if (userService.emailExists(email)) {

            messageLabel.setText(
                    "This email is already registered."
            );

            return;
        }

        Patient patient = new Patient(
                DataStore.getNextUserId(),
                fullName,
                email,
                password,
                dateOfBirth,
                phoneNumber,
                address
        );

        patient.setPwd(pwd);
        patient.setPwdIdNumber(pwdId);

        if (userService.register(patient)) {

            messageLabel.setText(
                    "Registration successful. You can now log in."
            );

            clearFields();

            RegistrationPopup.showPatient(messageLabel.getScene(), fullName, patient.getPatientId());

        } else {

            messageLabel.setText(
                    "Registration failed. That name or email is already registered."
            );
        }
    }

    private boolean isStrongPassword(String password) {

        if (password.length() < 8) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasNumber = false;
        boolean hasSpecial = false;

        for (char character : password.toCharArray()) {

            if (Character.isUpperCase(character)) {
                hasUppercase = true;
            }

            else if (Character.isLowerCase(character)) {
                hasLowercase = true;
            }

            else if (Character.isDigit(character)) {
                hasNumber = true;
            }

            else {
                hasSpecial = true;
            }
        }

        return hasUppercase
                && hasLowercase
                && hasNumber
                && hasSpecial;
    }

    @FXML
    private void handleLogin(ActionEvent event) {

        openPage(event, "login.fxml");
    }

    @FXML
    private void handleBack(ActionEvent event) {

        openPage(event, "register.fxml");
    }

    private void clearFields() {

        fullNameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        dateOfBirthPicker.setValue(null);
        phoneNumberField.clear();
        addressField.clear();
        pwdCheckBox.setSelected(false);
        pwdIdField.clear();
    }

    /** PWD ID formats differ by city, so only the basic shape is checked. */
    private boolean isValidPwdId(String id) {
        return id.matches("[A-Za-z0-9][A-Za-z0-9 -]{2,28}[A-Za-z0-9]");
    }

    private void openPage(ActionEvent event, String page) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/teleconsultation/system/portal/" + page
                    )
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            ph.edu.teleconsultation.system.portal.MainApp.showRoot(root);
            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}