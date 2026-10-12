package ph.edu.liceo.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.service.PasswordService;

/** The "Change Password" card opened from the Profile page. */
public class ChangePasswordController {

    @FXML private PasswordField currentField;
    @FXML private TextField currentVisibleField;
    @FXML private PasswordField newField;
    @FXML private TextField newVisibleField;
    @FXML private PasswordField confirmField;
    @FXML private TextField confirmVisibleField;
    @FXML private CheckBox showPasswordCheckBox;
    @FXML private Label messageLabel;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;

    private final PasswordService passwordService = new PasswordService();

    private User user;
    private Runnable onClose;

    @FXML
    private void initialize() {
        // Whichever field is on screen (hidden or shown) always holds the same text.
        PasswordToggle.link(currentField, currentVisibleField);
        PasswordToggle.link(newField, newVisibleField);
        PasswordToggle.link(confirmField, confirmVisibleField);
    }

    void configure(User user, Runnable onClose) {
        this.user = user;
        this.onClose = onClose;
    }

    void focusFirst() {
        currentField.requestFocus();
    }

    @FXML
    private void handleShowPassword() {
        boolean show = showPasswordCheckBox.isSelected();

        PasswordToggle.setShown(show, currentField, currentVisibleField);
        PasswordToggle.setShown(show, newField, newVisibleField);
        PasswordToggle.setShown(show, confirmField, confirmVisibleField);
    }

    /** Enter in a field moves to the next one; Enter in the last field saves. */
    @FXML
    private void handleCurrentEnter() {
        (showPasswordCheckBox.isSelected() ? newVisibleField : newField).requestFocus();
    }

    @FXML
    private void handleNewEnter() {
        (showPasswordCheckBox.isSelected() ? confirmVisibleField : confirmField).requestFocus();
    }

    @FXML
    private void handleSave() {

        PasswordService.Result result = passwordService.changePassword(
                user,
                currentField.getText(),
                newField.getText(),
                confirmField.getText());

        if (!result.isOk()) {
            messageLabel.setText(result.getMessage());
            return;
        }

        // The "Password changed" pop-up comes from the notification the service created.
        clearFields();
        close();
    }

    @FXML
    private void handleCancel() {
        clearFields();
        close();
    }

    private void clearFields() {
        currentField.clear();
        newField.clear();
        confirmField.clear();
        messageLabel.setText("");
    }

    private void close() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
