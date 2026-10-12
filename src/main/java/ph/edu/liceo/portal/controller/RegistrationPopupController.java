package ph.edu.liceo.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The card shown after registering: "you're registered" or "waiting for verification". */
public class RegistrationPopupController {

    @FXML private VBox card;
    @FXML private StackPane iconCircle;
    @FXML private Label iconLabel;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private VBox stepsBox;
    @FXML private HBox stepThreeRow;
    @FXML private Label stepThreeLabel;
    @FXML private Button closeButton;
    @FXML private Button loginButton;

    private Runnable onLogin;
    private Runnable onClose;

    /** Patient accounts are active straight away. */
    void showRegistered(String name, String patientId, Runnable onLogin, Runnable onClose) {
        this.onLogin = onLogin;
        this.onClose = onClose;

        iconLabel.setText("✓");
        titleLabel.setText("You're Registered!");
        messageLabel.setText("Welcome to TeleConsult, " + name
                + ". Your patient account is active, so you can sign in right now.\n\nYour Patient ID is "
                + patientId + ". Keep it safe, you will need it if you ever forget your password.");

        stepsBox.setVisible(false);
        stepsBox.setManaged(false);

        closeButton.setText("Stay Here");
        loginButton.setText("Go to Login");
    }

    /** Doctors, nurses and secretaries must wait for the administrator. */
    void showWaiting(String name, String role, Runnable onLogin, Runnable onClose) {
        this.onLogin = onLogin;
        this.onClose = onClose;

        iconCircle.getStyleClass().remove("popup-icon-success");
        iconCircle.getStyleClass().add("popup-icon-pending");

        iconLabel.setText("...");
        titleLabel.setText("Waiting for Verification");
        messageLabel.setText("Thank you, " + name + "! Your " + role
                + " registration was received. An administrator has to verify your account "
                + "before you can sign in.");

        stepThreeLabel.setText("You can sign in once you are approved");

        closeButton.setText("Close");
        loginButton.setText("Back to Login");
    }

    void setCloseAction(Runnable onClose) {
        this.onClose = onClose;
    }

    @FXML
    private void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
    }

    @FXML
    private void handleLogin() {
        if (onLogin != null) {
            onLogin.run();
        }
    }
}
