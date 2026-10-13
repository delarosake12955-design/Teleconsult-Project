package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import ph.edu.teleconsultation.system.portal.model.Appointment;

/**
 * Opens the "Return to patient" card (return-request.fxml) on top of the current page, the same
 * way the Vitals check-in card appears. Esc, Cancel and the close button do nothing.
 */
public final class ReturnRequestDialog {

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/return-request.fxml";

    private ReturnRequestDialog() {
    }

    /** One or several requests share one reason. {@code onReturn} receives the reason (may be empty) only when the nurse presses "Return to patient". */
    public static void show(Scene scene, List<Appointment> appointments, Consumer<String> onReturn) {
        try {
            FXMLLoader loader = new FXMLLoader(ReturnRequestDialog.class.getResource(FXML));
            Parent card = loader.load();
            ReturnRequestController controller = loader.getController();

            Node[] layer = new Node[1];
            controller.configure(appointments, onReturn, () -> OverlayHost.hide(layer[0]));

            layer[0] = OverlayHost.showDialog(scene, card);
            Platform.runLater(controller::focusDefault);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
