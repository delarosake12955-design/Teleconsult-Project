package ph.edu.liceo.portal.controller;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class RegisterController {

    @FXML
    private void handlePatient(ActionEvent event) {
        openPage(event, "patient-register.fxml");
    }

    @FXML
    private void handleDoctor(ActionEvent event) {
        openPage(event, "doctor-register.fxml");
    }

    @FXML
    private void handleNurse(ActionEvent event) {
        openPage(event, "nurse-register.fxml");
    }

    @FXML
    private void handleSecretary(ActionEvent event) {
        openPage(event, "secretary-register.fxml");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        openPage(event, "login.fxml");
    }

    @FXML
    private void handleHome(ActionEvent event) {
        openPage(event, "homepage.fxml");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        openPage(event, "homepage.fxml");
    }

    private void openPage(ActionEvent event, String page) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/liceo/portal/" + page
                    )
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            ph.edu.liceo.portal.MainApp.showRoot(root);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}