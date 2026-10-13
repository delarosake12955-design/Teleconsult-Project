// GIVEN COMPLETE - do not change this file.
// "opens ... to javafx.fxml" is what lets FXML fill in your @FXML fields.
module ph.edu.teleconsultation.system.portal {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;   // javax.sound.sampled, used by NotificationSound


    opens ph.edu.teleconsultation.system.portal to javafx.fxml;
    opens ph.edu.teleconsultation.system.portal.controller to javafx.fxml;

    exports ph.edu.teleconsultation.system.portal;
}
