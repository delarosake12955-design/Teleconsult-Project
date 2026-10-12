package ph.edu.liceo.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import ph.edu.liceo.portal.model.Admin;
import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.Nurse;
import ph.edu.liceo.portal.model.Patient;
import ph.edu.liceo.portal.model.Secretary;
import ph.edu.liceo.portal.model.User;

/** "Log out?" card with Yes / No. */
public class LogoutConfirmController {

    @FXML private Label userLabel;
    @FXML private Button noButton;
    @FXML private Button yesButton;

    private Runnable onYes;
    private Runnable onNo;

    void configure(User user, Runnable onYes, Runnable onNo) {
        this.onYes = onYes;
        this.onNo = onNo;

        if (user != null) {
            userLabel.setText(user.getFullName() + "  •  " + roleOf(user));
        }
    }

    /** "No" is focused so a stray Enter never logs anyone out. */
    void focusDefault() {
        noButton.requestFocus();
    }

    @FXML
    private void handleYes() {
        if (onYes != null) {
            onYes.run();
        }
    }

    @FXML
    private void handleNo() {
        if (onNo != null) {
            onNo.run();
        }
    }

    private String roleOf(User user) {
        if (user instanceof Doctor) return "Doctor";
        if (user instanceof Nurse) return "Nurse";
        if (user instanceof Secretary) return "Secretary";
        if (user instanceof Admin) return "Admin";
        if (user instanceof Patient) return "Patient";
        return "User";
    }
}
