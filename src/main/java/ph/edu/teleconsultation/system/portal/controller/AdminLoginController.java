package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import ph.edu.teleconsultation.system.portal.model.Admin;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.service.AuthenticationService;

public class AdminLoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private CheckBox showPasswordCheckBox;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Button backButton;

    private AuthenticationService authenticationService =
            new AuthenticationService();

    @FXML
    private void initialize() {

        // Enter signs in without clicking the button.
        emailField.setOnAction(event -> {
            if (typedPassword().isEmpty()) {
                (showPasswordCheckBox.isSelected() ? visiblePasswordField : passwordField).requestFocus();
            } else {
                submitFromKeyboard();
            }
        });
        passwordField.setOnAction(event -> submitFromKeyboard());
        visiblePasswordField.setOnAction(event -> submitFromKeyboard());
    }

    private String typedPassword() {

        return showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText()
                : passwordField.getText();
    }

    private void submitFromKeyboard() {

        if (!loginButton.isDisabled()) {

            handleLogin();
        }
    }

    @FXML
    private void handleShowPassword() {

        if (showPasswordCheckBox.isSelected()) {

            visiblePasswordField.setText(
                    passwordField.getText()
            );

            passwordField.setVisible(false);
            passwordField.setManaged(false);

            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);

            visiblePasswordField.requestFocus();

            visiblePasswordField.positionCaret(
                    visiblePasswordField.getText().length()
            );

        } else {

            passwordField.setText(
                    visiblePasswordField.getText()
            );

            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);

            passwordField.setVisible(true);
            passwordField.setManaged(true);

            passwordField.requestFocus();

            passwordField.positionCaret(
                    passwordField.getText().length()
            );
        }
    }

    @FXML
    private void handleLogin() {

        String email =
                emailField.getText().trim();

        String password;

        if (showPasswordCheckBox.isSelected()) {

            password =
                    visiblePasswordField.getText();

        } else {

            password =
                    passwordField.getText();
        }

        if (email.isEmpty() || password.isEmpty()) {

            messageLabel.setText(
                    "Please enter your admin email and password."
            );

            return;
        }

        if (authenticationService.isLockedOut(email)) {

            messageLabel.setText(
                    "Too many failed attempts. Please wait a minute and try again."
            );

            return;
        }

        User user =
                authenticationService.login(
                        email,
                        password
                );

        if (user == null) {

            messageLabel.setText(
                    "Incorrect email or password."
            );

            passwordField.clear();
            visiblePasswordField.clear();

            return;
        }

        if (!(user instanceof Admin)) {

            messageLabel.setText(
                    "This account is not an administrator account."
            );

            return;
        }

        LoginController.setLoggedInUser(user);
        messageLabel.setText("");

        loginButton.setDisable(true);

        LoadingOverlay.show(
                loginButton.getScene(),
                user,
                this::openAdminDashboard
        );
    }

    @FXML
    private void handleBack(ActionEvent event) {

        openPage(
                event,
                "login.fxml"
        );
    }

    private void openAdminDashboard() {

        try {

            Parent root =
                    FXMLLoader.load(
                            getClass().getResource(
                                    "/ph/edu/teleconsultation/system/portal/admin-dashboard.fxml"
                            )
                    );

            Stage stage =
                    (Stage) loginButton
                            .getScene()
                            .getWindow();

            ph.edu.teleconsultation.system.portal.MainApp.showRoot(root);
            stage.show();

            OverlayHost.fadeIn(root);
            NotificationToast.showUnreadSummary();

        } catch (IOException e) {

            e.printStackTrace();

            loginButton.setDisable(false);
        }
    }

    private void openPage(
            ActionEvent event,
            String page
    ) {

        try {

            Parent root =
                    FXMLLoader.load(
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