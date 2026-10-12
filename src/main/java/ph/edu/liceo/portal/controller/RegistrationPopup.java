package ph.edu.liceo.portal.controller;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import ph.edu.liceo.portal.MainApp;

/** Opens the registration result card on top of the registration page. */
public final class RegistrationPopup {

    private static final String FXML = "/ph/edu/liceo/portal/registration-popup.fxml";
    private static final String LOGIN = "/ph/edu/liceo/portal/login.fxml";

    private RegistrationPopup() {
    }

    /** Patient: the account is active and has a generated Patient ID. */
    public static void showPatient(Scene scene, String name, String patientId) {
        open(scene, controller -> controller.showRegistered(name, patientId, RegistrationPopup::goToLogin, null));
    }

    /** Doctor / nurse / secretary: the account waits for administrator verification. */
    public static void showStaff(Scene scene, String name, String role) {
        open(scene, controller -> controller.showWaiting(name, role, RegistrationPopup::goToLogin, null));
    }

    private interface Setup {
        void apply(RegistrationPopupController controller);
    }

    private static void open(Scene scene, Setup setup) {
        if (scene == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(RegistrationPopup.class.getResource(FXML));
            Parent card = loader.load();
            RegistrationPopupController controller = loader.getController();

            setup.apply(controller);

            Node[] dialog = new Node[1];
            dialog[0] = OverlayHost.showDialog(scene, card);

            // "Stay Here" / "Close" only closes the card.
            controller.setCloseAction(() -> OverlayHost.hide(dialog[0]));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void goToLogin() {
        try {
            Parent login = FXMLLoader.load(RegistrationPopup.class.getResource(LOGIN));

            MainApp.showRoot(login);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
