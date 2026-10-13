package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;
import java.util.List;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import ph.edu.teleconsultation.system.portal.model.Appointment;

/** Pops the "Appointment limit reached" card on top of the page. Esc, Got it and the X all close it. */
public final class LimitReachedDialog {

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/limit-reached.fxml";

    private LimitReachedDialog() {
    }

    public static void show(Scene scene, List<Appointment> active, int max) {
        try {
            FXMLLoader loader = new FXMLLoader(LimitReachedDialog.class.getResource(FXML));
            Parent card = loader.load();
            LimitReachedController controller = loader.getController();

            Node[] layer = new Node[1];
            controller.configure(active, max, () -> OverlayHost.hide(layer[0]));

            layer[0] = OverlayHost.showDialog(scene, card);
            Platform.runLater(controller::focusDefault);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
