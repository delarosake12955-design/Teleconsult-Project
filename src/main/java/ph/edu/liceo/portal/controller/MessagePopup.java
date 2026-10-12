package ph.edu.liceo.portal.controller;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

/** Shows a success card in the middle of the window. */
public final class MessagePopup {

    private static final String FXML = "/ph/edu/liceo/portal/message-popup.fxml";

    private MessagePopup() {
    }

    /** @param onOk runs after the card closes (may be null) */
    public static void show(Scene scene, String title, String message, String buttonText, Runnable onOk) {

        if (scene == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(MessagePopup.class.getResource(FXML));
            Parent card = loader.load();
            MessagePopupController controller = loader.getController();

            Node[] dialog = new Node[1];

            controller.configure(title, message, buttonText, () -> {
                OverlayHost.hide(dialog[0]);

                if (onOk != null) {
                    onOk.run();
                }
            });

            dialog[0] = OverlayHost.showDialog(scene, card);

            Platform.runLater(controller::focusButton);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
