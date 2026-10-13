package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import ph.edu.teleconsultation.system.portal.model.Feedback;

/** Opens the feedback list card for a doctor (their own) or the admin (everyone's). */
public final class FeedbackListDialog {

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/feedback-list.fxml";

    private FeedbackListDialog() {
    }

    public static void show(Scene scene, String title, String subtitle, List<Feedback> items, boolean showDoctor) {
        try {
            FXMLLoader loader = new FXMLLoader(FeedbackListDialog.class.getResource(FXML));
            Parent card = loader.load();
            FeedbackListController controller = loader.getController();

            Node[] layer = new Node[1];
            controller.configure(title, subtitle, items, showDoctor, () -> OverlayHost.hide(layer[0]));

            layer[0] = OverlayHost.showDialog(scene, card);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
