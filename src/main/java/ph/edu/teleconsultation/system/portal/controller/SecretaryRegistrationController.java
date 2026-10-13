package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.service.UserService;

public class SecretaryRegistrationController {

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
    private TextField departmentField;

    @FXML
    private Label messageLabel;

    private UserService userService = new UserService();

    @FXML
    private void initialize() {

        PasswordToggle.link(passwordField, visiblePasswordField);
        PasswordToggle.link(confirmPasswordField, visibleConfirmPasswordField);
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
        String department = departmentField.getText().trim();

        if (fullName.isEmpty()
                || email.isEmpty()
                || password.isEmpty()
                || department.isEmpty()) {

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

        Secretary secretary = new Secretary(
                DataStore.getNextUserId(),
                fullName,
                email,
                password,
                department
        );

        if (userService.register(secretary)) {

            messageLabel.setText(
                    "Registration submitted. Please wait for administrator approval."
            );

            clearFields();

            RegistrationPopup.showStaff(messageLabel.getScene(), fullName, "Secretary");

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

            } else if (Character.isLowerCase(character)) {

                hasLowercase = true;

            } else if (Character.isDigit(character)) {

                hasNumber = true;

            } else {

                hasSpecial = true;
            }
        }

        return hasUppercase
                && hasLowercase
                && hasNumber
                && hasSpecial;
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
        departmentField.clear();
    }

    private void openPage(ActionEvent event, String page) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/teleconsultation/system/portal/" + page
                    )
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            ph.edu.teleconsultation.system.portal.MainApp.showRoot(root);
            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}