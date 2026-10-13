package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

import ph.edu.teleconsultation.system.portal.MainApp;
import ph.edu.teleconsultation.system.portal.model.Notification;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.service.NotificationService;

/**
 * Pops a notification up on screen the moment the signed-in user receives it, and
 * once after sign-in for anything that arrived while they were away.
 */
public final class NotificationToast {

    private static final String FXML = "/ph/edu/teleconsultation/system/portal/notification-toast.fxml";

    private static final NotificationService SERVICE = new NotificationService();

    private static boolean installed;

    private NotificationToast() {
    }

    /** Call once at start-up: every notification created from now on is watched. */
    public static void install() {
        if (installed) {
            return;
        }

        installed = true;

        // Get the chimes and the pop-up layout ready now, so the first
        // notification appears and sounds right on time.
        NotificationSound.warmUp();
        Platform.runLater(NotificationToast::preload);

        NotificationService.addListener(notification ->
                Platform.runLater(() -> onReceived(notification)));
    }

    private static void onReceived(Notification notification) {
        User current = LoginController.getLoggedInUser();

        // Only the person who is signed in right now can see a pop-up.
        if (current == null || notification.getRecipient() != current) {
            return;
        }

        show(notification);

        Scene scene = currentScene();

        if (scene != null) {
            NotificationController.refreshBadge(scene);
        }
    }

    /** Loads the layout once off-screen so its classes and style sheet are ready. */
    private static void preload() {
        try {
            Parent card = new FXMLLoader(NotificationToast.class.getResource(FXML)).load();
            new Scene(card);
            card.applyCss();
        } catch (Exception | LinkageError e) {
            // Only a head start; the real pop-up still loads normally.
        }
    }

    /** After sign-in: one pop-up telling how many notifications are waiting. */
    public static void showUnreadSummary() {
        User user = LoginController.getLoggedInUser();

        if (user == null) {
            return;
        }

        int unread = SERVICE.getUnreadCount(user);

        if (unread == 0) {
            return;
        }

        String text = unread == 1
                ? "You have 1 unread notification. Tap to read it."
                : "You have " + unread + " unread notifications. Tap to read them.";

        // Let the dashboard finish fading in first.
        PauseTransition wait = new PauseTransition(Duration.millis(650));
        wait.setOnFinished(event -> {
            if (LoginController.getLoggedInUser() == user) {
                show(new Notification(0, user, "Welcome back!", text,
                        Notification.Type.INFO));
            }
        });
        wait.play();
    }

    /** Shows one pop-up (nothing is stored; the inbox is untouched). */
    public static void show(Notification notification) {
        Scene scene = currentScene();

        if (scene == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(NotificationToast.class.getResource(FXML));
            Parent card = loader.load();
            ToastController controller = loader.getController();

            controller.configure(notification, () -> openInbox(card));

            OverlayHost.addToast(scene, card);

            // The chime starts in the same instant the pop-up (and its bell)
            // begins to move, so sound and picture are always together.
            NotificationSound.play(notification.getType());
            controller.play();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Removes every pop-up (used on logout so the next person never sees them). */
    public static void clearAll() {
        OverlayHost.clearToasts(currentScene());
    }

    /** Clicking a pop-up opens the notification inbox inside the dashboard. */
    private static void openInbox(Node source) {
        Scene scene = source.getScene();

        if (scene == null || scene.lookup("#contentHost") == null) {
            return;
        }

        Navigator.open(source, "notifications.fxml");
        NotificationController.refreshBadge(scene);
    }

    private static Scene currentScene() {
        Stage stage = MainApp.getStage();

        return stage == null ? null : stage.getScene();
    }
}
