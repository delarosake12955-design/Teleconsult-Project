package ph.edu.liceo.portal.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.Nurse;
import ph.edu.liceo.portal.model.Patient;
import ph.edu.liceo.portal.model.Secretary;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.service.AuthenticationService;

public class LoginController {

    @FXML
    private TextField usernameField;

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
    private Button registerButton;

    @FXML
    private Button homeButton;

    @FXML
    private Button backButton;

    @FXML
    private Button forgotButton;

    private String selectedRole = "Patient";

    private AuthenticationService authenticationService =
            new AuthenticationService();

    private static User loggedInUser;

    @FXML
    private void initialize() {

        // login-patient / -doctor / -nurse / -secretary.fxml each set the role on the button.
        if (loginButton.getUserData() instanceof String role && !role.isBlank()) {
            selectedRole = role;
        }

        // Enter signs in without clicking the button. In the email box, Enter first
        // moves to the password if it is still empty.
        usernameField.setOnAction(event -> {
            if (typedPassword().isEmpty()) {
                (showPasswordCheckBox.isSelected() ? visiblePasswordField : passwordField).requestFocus();
            } else {
                submitFromKeyboard();
            }
        });
        passwordField.setOnAction(event -> submitFromKeyboard());
        visiblePasswordField.setOnAction(event -> submitFromKeyboard());

        // The window now keeps one shared Scene, so the shortcut must be
        // removed when this page is left (otherwise old copies pile up and
        // crash), and it is a *filter* so a focused text box cannot swallow
        // Ctrl+Shift+A before we see it.
        loginButton.sceneProperty().addListener(
                (observable, oldScene, newScene) -> {

                    if (oldScene != null) {
                        oldScene.removeEventFilter(
                                KeyEvent.KEY_PRESSED,
                                adminShortcut
                        );
                    }

                    if (newScene != null) {
                        newScene.addEventFilter(
                                KeyEvent.KEY_PRESSED,
                                adminShortcut
                        );
                    }
                }
        );
    }

    private final EventHandler<KeyEvent> adminShortcut =
            this::handleAdminShortcut;

    private void handleAdminShortcut(KeyEvent event) {

        if (loginButton.getScene() == null) {
            return;
        }

        if (event.getCode() == KeyCode.A
                && (event.isControlDown() || event.isShortcutDown())
                && event.isShiftDown()) {

            event.consume();

            openAdminLogin();
        }
    }

    private void openAdminLogin() {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/liceo/portal/admin-login.fxml"
                    )
            );

            Stage stage =
                    (Stage) loginButton
                            .getScene()
                            .getWindow();

            ph.edu.liceo.portal.MainApp.showRoot(root);
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();

            messageLabel.setText("Could not open the admin login.");
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

        String username =
                usernameField.getText().trim();

        String password;

        if (showPasswordCheckBox.isSelected()) {

            password =
                    visiblePasswordField.getText();

        } else {

            password =
                    passwordField.getText();
        }

        if (username.isEmpty() || password.isEmpty()) {

            messageLabel.setText(
                    "Please enter your email and password."
            );

            return;
        }

        if (authenticationService.isLockedOut(username)) {

            messageLabel.setText(
                    "Too many failed attempts. Please wait a minute and try again."
            );

            return;
        }

        User user =
                authenticationService.login(
                        username,
                        password
                );

        if (user == null) {

            messageLabel.setText(
                    "Incorrect email or password, or your account is not active."
            );

            passwordField.clear();
            visiblePasswordField.clear();

            return;
        }

        if (!isCorrectRole(user)) {

            messageLabel.setText(
                    "This is not a " + selectedRole + " account. Use \"Choose role\" to sign in as someone else."
            );

            return;
        }

        loggedInUser = user;

        messageLabel.setText("");

        // Role-specific loading screen, then the dashboard opens.
        loginButton.setDisable(true);

        LoadingOverlay.show(
                loginButton.getScene(),
                user,
                () -> openDashboard(user)
        );
    }

    /** Opens the registration form that matches this sign-in page's role. */
    @FXML
    private void handleRegister(ActionEvent event) {

        openPage(
                event,
                selectedRole.toLowerCase() + "-register.fxml"
        );
    }

    /** "Not a doctor? Sign in as ..." chips: open another role's sign-in page. */
    @FXML
    private void switchRole(ActionEvent event) {

        Object target = ((Node) event.getSource()).getUserData();

        if (target instanceof String role && !role.isBlank()) {

            openPage(
                    event,
                    "login-" + role.toLowerCase() + ".fxml"
            );
        }
    }

    /** Back to the "Sign In As Your Role" page. */
    @FXML
    private void handleBack(ActionEvent event) {

        openPage(
                event,
                "login.fxml"
        );
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {

        openPage(
                event,
                "forgot-password-" + selectedRole.toLowerCase() + ".fxml"
        );
    }

    private String typedPassword() {

        return showPasswordCheckBox.isSelected()
                ? visiblePasswordField.getText()
                : passwordField.getText();
    }

    /** Enter key: ignored while the loading screen is already running. */
    private void submitFromKeyboard() {

        if (!loginButton.isDisabled()) {

            handleLogin();
        }
    }

    @FXML
    private void handleHome(ActionEvent event) {

        openPage(
                event,
                "homepage.fxml"
        );
    }

    private boolean isCorrectRole(User user) {

        if (selectedRole.equals("Patient")) {

            return user instanceof Patient;
        }

        if (selectedRole.equals("Doctor")) {

            return user instanceof Doctor;
        }

        if (selectedRole.equals("Nurse")) {

            return user instanceof Nurse;
        }

        if (selectedRole.equals("Secretary")) {

            return user instanceof Secretary;
        }

        return false;
    }

    private void openDashboard(User user) {

        String page = "";

        if (user instanceof Patient) {

            page = "patient-dashboard.fxml";

        } else if (user instanceof Doctor) {

            page = "doctor-dashboard.fxml";

        } else if (user instanceof Nurse) {

            page = "nurse-dashboard.fxml";

        } else if (user instanceof Secretary) {

            page = "secretary-dashboard.fxml";
        }

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/liceo/portal/" + page
                    )
            );

            Stage stage =
                    (Stage) loginButton
                            .getScene()
                            .getWindow();

            ph.edu.liceo.portal.MainApp.showRoot(root);
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

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/liceo/portal/" + page
                    )
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            ph.edu.liceo.portal.MainApp.showRoot(root);
            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private void clearMessage() {

        if (messageLabel != null) {

            messageLabel.setText("");
        }
    }

    public static User getLoggedInUser() {

        return loggedInUser;
    }

    public static void setLoggedInUser(User user) {
        loggedInUser = user;
    }

    public static void clearLoggedInUser() {

        loggedInUser = null;
    }
}