package ph.edu.liceo.portal.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import ph.edu.liceo.portal.model.Notification;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.service.NotificationService;

/**
 * Notification inbox shared by every role. Unread items show a red dot, and
 * the red badge on the sidebar "Notifications" button shows how many are unread.
 */
public class NotificationController {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private static final NotificationService service = new NotificationService();

    @FXML
    private VBox notificationList;

    @FXML
    private Label summaryLabel;

    @FXML
    private Button markAllButton;

    @FXML
    private Button clearButton;

    @FXML
    private Button soundButton;

    @FXML
    private void initialize() {
        updateSoundButton();
        reload();
    }

    @FXML
    private void handleToggleSound() {
        NotificationSound.setEnabled(!NotificationSound.isEnabled());
        updateSoundButton();

        if (NotificationSound.isEnabled()) {
            NotificationSound.play(Notification.Type.INFO);   // preview what it sounds like
        }
    }

    private void updateSoundButton() {
        soundButton.setText(NotificationSound.isEnabled() ? "Sound: On" : "Sound: Off");
    }

    @FXML
    private void handleMarkAllRead() {
        User user = LoginController.getLoggedInUser();

        if (user != null) {
            service.markAllRead(user);
        }

        reload();
    }

    @FXML
    private void handleClearAll() {
        User user = LoginController.getLoggedInUser();

        if (user != null) {
            service.clearAll(user);
        }

        reload();
    }

    private void reload() {

        User user = LoginController.getLoggedInUser();
        List<Notification> items = user == null ? List.of() : service.getNotifications(user);
        int unread = user == null ? 0 : service.getUnreadCount(user);

        notificationList.getChildren().clear();

        if (items.isEmpty()) {
            summaryLabel.setText("You're all caught up.");
            notificationList.getChildren().add(buildEmptyState());
        } else {
            summaryLabel.setText(unread == 0
                    ? "All " + items.size() + " notifications read"
                    : unread + " unread \u2022 " + items.size() + " total");

            for (Notification item : items) {
                notificationList.getChildren().add(buildCard(user, item));
            }
        }

        markAllButton.setDisable(unread == 0);
        clearButton.setDisable(items.isEmpty());

        refreshBadge(notificationList.getScene());
    }

    private Node buildCard(User user, Notification item) {

        Region dot = new Region();
        dot.getStyleClass().add(item.isRead() ? "notification-dot-read" : "notification-dot");

        Label title = new Label(item.getTitle());
        title.getStyleClass().add(item.isRead() ? "notification-title-read" : "notification-title");

        Label message = new Label(item.getMessage());
        message.setWrapText(true);
        message.getStyleClass().add("notification-message");

        Label time = new Label(item.getCreatedAt().format(TIME_FORMAT));
        time.getStyleClass().add("notification-time");

        VBox text = new VBox(4, title, message, time);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button dismiss = new Button("Dismiss");
        dismiss.getStyleClass().add("notification-dismiss");
        dismiss.setOnAction(event -> {
            service.delete(user, item);
            reload();
            event.consume();
        });

        HBox card = new HBox(14, dot, text, dismiss);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("notification-card",
                "notification-type-" + item.getType().name().toLowerCase());

        if (!item.isRead()) {
            card.getStyleClass().add("notification-card-unread");
        }

        // Clicking a card marks it as read.
        card.setOnMouseClicked(event -> {
            if (!item.isRead()) {
                service.markRead(item);
                reload();
            }
        });

        return card;
    }

    private Node buildEmptyState() {

        Label title = new Label("No notifications yet");
        title.getStyleClass().add("notification-empty-title");

        Label text = new Label("Updates about appointments, consultations and records will show up here.");
        text.getStyleClass().add("notification-empty-text");

        VBox box = new VBox(8, title, text);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("notification-empty");

        return box;
    }

    // ------------------------------------------------------------------
    // Red badge on the sidebar button
    // ------------------------------------------------------------------

    /** Shows the red badge with the unread count, or hides it when there is none. */
    public static void applyBadge(Label badge) {

        if (badge == null) {
            return;
        }

        User user = LoginController.getLoggedInUser();
        int unread = user == null ? 0 : service.getUnreadCount(user);

        badge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        badge.setVisible(unread > 0);
        badge.setManaged(unread > 0);
    }

    /** Finds the badge in the dashboard that contains this scene and updates it. */
    public static void refreshBadge(Scene scene) {

        if (scene != null && scene.lookup("#notificationBadge") instanceof Label badge) {
            applyBadge(badge);
        }
    }
}
