package ph.edu.liceo.portal.controller;

import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Show / hide password support. Each password has a hidden PasswordField and
 * a plain TextField; their text is kept identical, so whichever one is on
 * screen always holds the current value.
 */
final class PasswordToggle {

    private PasswordToggle() {
    }

    static void link(PasswordField hidden, TextField shown) {

        if (hidden != null && shown != null) {
            shown.textProperty().bindBidirectional(hidden.textProperty());
        }
    }

    static void setShown(boolean show, PasswordField hidden, TextField shown) {

        if (hidden == null || shown == null) {
            return;
        }

        shown.setVisible(show);
        shown.setManaged(show);

        hidden.setVisible(!show);
        hidden.setManaged(!show);
    }
}
