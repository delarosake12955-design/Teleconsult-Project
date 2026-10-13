package ph.edu.teleconsultation.system.portal.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Shared behaviour of every role dashboard: the sidebar stays on screen and
 * pages are swapped into the content area.
 */
public abstract class DashboardShell {

    @FXML
    protected StackPane contentHost;

    @FXML
    protected Node dashboardContent;

    /** Red badge on the sidebar "Notifications" button (unread count). */
    @FXML
    protected Label notificationBadge;

    /** Shows a page inside the dashboard, or the dashboard home itself. */
    protected void showPage(String page) {
        if (contentHost == null) {
            return;
        }

        if (page.endsWith("-dashboard.fxml")) {
            contentHost.getChildren().setAll(dashboardContent);
            refreshNotificationBadge();
            return;
        }

        Navigator.open(contentHost, page);
        refreshNotificationBadge();
    }

    @FXML
    protected void handleNotifications(ActionEvent event) {
        showPage("notifications.fxml");
    }

    /** Updates the red unread badge; call whenever the dashboard (re)loads. */
    protected void refreshNotificationBadge() {
        NotificationController.applyBadge(notificationBadge);
    }

    /** Sets a label's text; labels missing from a given FXML are simply skipped. */
    protected static void set(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    @FXML
    protected void handleLogout(ActionEvent event) {
        Navigator.logout(contentHost);
    }
}
