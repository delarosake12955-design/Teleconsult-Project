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
import ph.edu.teleconsultation.system.portal.model.Feedback;

/** Opens the patient's "How was your visit?" card. {@code onSend} runs only when they press Send. */
public final class FeedbackDialog {

    public record Choice(int rating, List<String> tags, String comment) {
    }

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/feedback-form.fxml";

    private FeedbackDialog() {
    }

    /** {@code existing} is the earlier feedback to edit, or null for a new one. */
    public static void show(Scene scene, Appointment appointment, Feedback existing, Consumer<Choice> onSend) {
        try {
            FXMLLoader loader = new FXMLLoader(FeedbackDialog.class.getResource(FXML));
            Parent card = loader.load();
            FeedbackFormController controller = loader.getController();

            Node[] layer = new Node[1];
            controller.configure(appointment, existing, onSend, () -> OverlayHost.hide(layer[0]));

            layer[0] = OverlayHost.showDialog(scene, card);
            Platform.runLater(controller::focusDefault);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
