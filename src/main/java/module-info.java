// GIVEN COMPLETE - do not change this file.
// "opens ... to javafx.fxml" is what lets FXML fill in your @FXML fields.
module ph.edu.liceo.portal {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;   // javax.sound.sampled, used by NotificationSound


    opens ph.edu.liceo.portal to javafx.fxml;
    opens ph.edu.liceo.portal.controller to javafx.fxml;

    exports ph.edu.liceo.portal;
}
