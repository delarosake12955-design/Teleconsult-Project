package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Esc key behaviour for the whole window:
 *
 *   1. if a pop-up card is open (change password, this question, ...), Esc closes it;
 *   2. otherwise, when someone is signed in and on a dashboard, Esc asks "Log out?".
 */
public final class LogoutConfirm {

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/logout-confirm.fxml";
    private static final String INSTALLED = "logout-confirm-installed";

    private LogoutConfirm() {
    }

    /** Call whenever the window's scene is set up; it only installs itself once per scene. */
    public static void install(Stage stage) {

        if (stage == null || stage.getScene() == null) {
            return;
        }

        // Esc must not also leave full screen (F11 does that, see MainApp).
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("Press F11 to exit full screen");

        Scene scene = stage.getScene();

        if (scene.getProperties().containsKey(INSTALLED)) {
            return;
        }

        scene.getProperties().put(INSTALLED, Boolean.TRUE);

        // A filter, so a focused text box cannot swallow Esc before we see it.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

            if (event.getCode() != KeyCode.ESCAPE) {
                return;
            }

            if (OverlayHost.closeTopDialog(scene)) {
                event.consume();
                return;
            }

            if (canAsk(scene)) {
                event.consume();
                show(scene);
            }
        });
    }

    private static boolean canAsk(Scene scene) {
        return LoginController.getLoggedInUser() != null
                && scene.lookup("#contentHost") != null;
    }

    public static void show(Scene scene) {

        try {
            FXMLLoader loader = new FXMLLoader(LogoutConfirm.class.getResource(FXML));
            Parent card = loader.load();
            LogoutConfirmController controller = loader.getController();

            Node[] dialog = new Node[1];

            controller.configure(
                    LoginController.getLoggedInUser(),
                    () -> Navigator.logout(card),
                    () -> OverlayHost.hide(dialog[0]));

            dialog[0] = OverlayHost.showDialog(scene, card);

            Platform.runLater(controller::focusDefault);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
