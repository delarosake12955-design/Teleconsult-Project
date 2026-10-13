package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * "Sign In As Your Role" page (login.fxml). Mirrors the registration role chooser and
 * sends the person to the sign-in page of the role they pick.
 */
public class LoginRoleController {

    @FXML
    private Node homeButton;

    @FXML
    private void initialize() {

        // Same hidden Ctrl+Shift+A administrator shortcut the sign-in page always had.
        homeButton.sceneProperty().addListener((observable, oldScene, newScene) -> {

            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, adminShortcut);
            }

            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, adminShortcut);
            }
        });
    }

    private final EventHandler<KeyEvent> adminShortcut = event -> {

        if (event.getCode() == KeyCode.A
                && (event.isControlDown() || event.isShortcutDown())
                && event.isShiftDown()) {

            event.consume();
            openPage(homeButton, "admin-login.fxml");
        }
    };

    @FXML
    private void handlePatient(ActionEvent event) {
        openPage(event, "login-patient.fxml");
    }

    @FXML
    private void handleDoctor(ActionEvent event) {
        openPage(event, "login-doctor.fxml");
    }

    @FXML
    private void handleNurse(ActionEvent event) {
        openPage(event, "login-nurse.fxml");
    }

    @FXML
    private void handleSecretary(ActionEvent event) {
        openPage(event, "login-secretary.fxml");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        openPage(event, "register.fxml");
    }

    @FXML
    private void handleHome(ActionEvent event) {
        openPage(event, "homepage.fxml");
    }

    private void openPage(ActionEvent event, String page) {
        openPage((Node) event.getSource(), page);
    }

    private void openPage(Node source, String page) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource("/ph/edu/teleconsultation/system/portal/" + page)
            );

            Stage stage = (Stage) source.getScene().getWindow();

            ph.edu.teleconsultation.system.portal.MainApp.showRoot(root);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
