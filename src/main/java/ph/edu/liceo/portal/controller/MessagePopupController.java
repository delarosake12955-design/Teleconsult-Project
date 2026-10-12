package ph.edu.liceo.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** A small success card with one button (used after a password reset). */
public class MessagePopupController {

    @FXML private Label titleLabel;
    @FXML private Label messageLabel;
    @FXML private Button okButton;

    private Runnable onOk;

    void configure(String title, String message, String buttonText, Runnable onOk) {
        this.onOk = onOk;

        titleLabel.setText(title);
        messageLabel.setText(message);
        okButton.setText(buttonText);
    }

    void focusButton() {
        okButton.requestFocus();
    }

    @FXML
    private void handleOk() {
        if (onOk != null) {
            onOk.run();
        }
    }
}
