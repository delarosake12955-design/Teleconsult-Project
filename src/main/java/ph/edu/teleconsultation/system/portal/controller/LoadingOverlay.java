package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import ph.edu.teleconsultation.system.portal.model.Admin;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.User;

/**
 * Loading screen shown after a successful sign-in. Each role gets its own badge
 * and its own "what is loading" lines, then the dashboard opens.
 */
public final class LoadingOverlay {

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/login-loading.fxml";

    private LoadingOverlay() {
    }

    /**
     * @param onDone opens the dashboard; it runs once the loading sequence has finished.
     *               If the screen cannot be built, it runs immediately.
     */
    public static void show(Scene scene, User user, Runnable onDone) {
        if (scene == null) {
            onDone.run();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(LoadingOverlay.class.getResource(FXML));
            Parent layer = loader.load();
            LoadingController controller = loader.getController();

            Node shown = OverlayHost.showLayer(scene, layer);

            controller.play(portalOf(user), keyOf(user), user.getFullName(), stepsFor(user),
                    () -> {
                        onDone.run();
                        // The dashboard replaces the page and clears dialogs; this covers a failed load.
                        OverlayHost.remove(shown);
                    });

        } catch (IOException e) {
            e.printStackTrace();
            onDone.run();
        }
    }

    private static String keyOf(User user) {
        if (user instanceof Doctor) return "doctor";
        if (user instanceof Nurse) return "nurse";
        if (user instanceof Secretary) return "secretary";
        if (user instanceof Admin) return "admin";
        return "patient";
    }

    private static String portalOf(User user) {
        return keyOf(user).toUpperCase() + " PORTAL";
    }

    private static List<String> stepsFor(User user) {
        if (user instanceof Doctor) {
            return List.of("Verifying credentials...", "Loading your appointment requests...",
                    "Syncing your schedule...", "Preparing your dashboard...");
        }

        if (user instanceof Nurse) {
            return List.of("Verifying credentials...", "Loading pending consultation requests...",
                    "Checking the vital signs queue...", "Preparing your dashboard...");
        }

        if (user instanceof Secretary) {
            return List.of("Verifying credentials...", "Loading doctor-approved requests...",
                    "Checking doctor schedules...", "Preparing your dashboard...");
        }

        if (user instanceof Admin) {
            return List.of("Verifying credentials...", "Loading the user overview...",
                    "Checking verification requests...", "Preparing your dashboard...");
        }

        return List.of("Verifying credentials...", "Loading your appointments...",
                "Checking your notifications...", "Preparing your dashboard...");
    }
}
