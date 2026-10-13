package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import ph.edu.teleconsultation.system.portal.model.User;

/** Opens the Change Password card for whoever is signed in. */
public final class ChangePasswordDialog {

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/change-password.fxml";

    private ChangePasswordDialog() {
    }

    public static void show(Scene scene) {

        User user = LoginController.getLoggedInUser();

        if (scene == null || user == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(ChangePasswordDialog.class.getResource(FXML));
            Parent card = loader.load();
            ChangePasswordController controller = loader.getController();

            Node[] dialog = new Node[1];

            controller.configure(user, () -> OverlayHost.hide(dialog[0]));

            dialog[0] = OverlayHost.showDialog(scene, card);

            Platform.runLater(controller::focusFirst);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
