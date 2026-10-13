package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.service.AppointmentService;

/**
 * Opens the "Vitals check-in" card (vitals-visit.fxml) on top of the current page, the same
 * way the logout and notification pop-ups appear. Esc closes it like the other cards.
 */
public final class VitalsVisitDialog {

    /** What the nurse chose. */
    public record Choice(LocalDate date, String time, String note) {
    }

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/vitals-visit.fxml";

    private VitalsVisitDialog() {
    }

    /**
     * Shows the card. {@code onSend} runs only if the nurse presses "Send to patient" with a
     * choice the service accepts; Cancel, the close button and Esc do nothing.
     * The caller has already checked that a day is available.
     */
    public static void show(Scene scene, Appointment appointment, AppointmentService service,
                            Consumer<Choice> onSend) {
        try {
            FXMLLoader loader = new FXMLLoader(VitalsVisitDialog.class.getResource(FXML));
            Parent card = loader.load();
            VitalsVisitController controller = loader.getController();

            Node[] layer = new Node[1];
            controller.configure(appointment, service, onSend, () -> OverlayHost.hide(layer[0]));

            layer[0] = OverlayHost.showDialog(scene, card);
            Platform.runLater(controller::focusDefault);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
