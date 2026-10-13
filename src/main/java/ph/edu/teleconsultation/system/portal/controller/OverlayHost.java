package ph.edu.teleconsultation.system.portal.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The window's scene root. It holds the current page and, above it, the things that
 * must stay on screen while pages change:
 *
 *   [0]       the page (login, dashboard, ...)
 *   [1..n-2]  dialogs and loading screens (removed whenever a new page is shown)
 *   [n-1]     the toast layer (notification pop-ups, always on top)
 *
 * Every page change goes through {@link #setPage(Scene, Parent)}.
 */
public final class OverlayHost {

    private static final String HOST_STYLE = "overlay-host";
    private static final String DIALOG_KEY = "overlay-dialog";
    private static final String CARD_KEY = "overlay-card";
    private static final String TOASTS_KEY = "overlay-toasts";
    private static final String THEME = "/ph/edu/teleconsultation/system/portal/popup-theme.css";
    private static final String POLISH = "/ph/edu/teleconsultation/system/portal/ui-polish.css";
    private static final String GLASS = "/ph/edu/teleconsultation/system/portal/app-glass.css";

    private static final int PAGE_SLOT = 0;
    private static final int MAX_TOASTS = 3;

    private OverlayHost() {
    }

    // ------------------------------------------------------------------ pages

    /** Shows a page in the scene without losing the toasts, and closes open dialogs. */
    public static void setPage(Scene scene, Parent page) {
        if (scene == null || page == null) {
            return;
        }

        StackPane host = host(scene);

        // Shared polish (button states, scroll bars, tooltips) for every role's pages.
        java.net.URL polish = OverlayHost.class.getResource(POLISH);
        if (polish != null && !page.getStylesheets().contains(polish.toExternalForm())) {
            page.getStylesheets().add(polish.toExternalForm());
        }

        // App-wide "register look": scoped by the tc-app class so it reaches every page.
        if (!page.getStyleClass().contains("tc-app")) {
            page.getStyleClass().add("tc-app");
        }
        java.net.URL glass = OverlayHost.class.getResource(GLASS);
        if (glass != null && !page.getStylesheets().contains(glass.toExternalForm())) {
            page.getStylesheets().add(glass.toExternalForm());
        }

        host.getChildren().removeIf(node -> node.getProperties().containsKey(DIALOG_KEY));
        host.getChildren().set(PAGE_SLOT, page);
    }

    /** Fades a freshly shown page in. */
    public static void fadeIn(Node page) {
        page.setOpacity(0);

        FadeTransition fade = new FadeTransition(Duration.millis(380), page);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    /** Returns the scene's host, wrapping the current root the first time. */
    private static StackPane host(Scene scene) {
        if (scene.getRoot() instanceof StackPane existing
                && existing.getStyleClass().contains(HOST_STYLE)) {
            return existing;
        }

        Parent currentPage = scene.getRoot();

        VBox toasts = new VBox(10);
        toasts.setAlignment(Pos.TOP_RIGHT);
        toasts.setPadding(new Insets(20));
        toasts.setPickOnBounds(false);
        toasts.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(toasts, Pos.TOP_RIGHT);

        StackPane host = new StackPane();
        host.getStyleClass().add(HOST_STYLE);
        host.getProperties().put(TOASTS_KEY, toasts);
        host.getChildren().addAll(new Pane(), toasts);

        scene.setRoot(host);
        host.getChildren().set(PAGE_SLOT, currentPage);

        return host;
    }

    // ----------------------------------------------------------------- toasts

    public static void addToast(Scene scene, Node toast) {
        VBox layer = toastLayer(scene);

        layer.getChildren().add(0, toast);

        // Never stack more than a few; the oldest one just goes away.
        while (layer.getChildren().size() > MAX_TOASTS) {
            layer.getChildren().remove(layer.getChildren().size() - 1);
        }
    }

    public static void clearToasts(Scene scene) {
        if (scene != null) {
            toastLayer(scene).getChildren().clear();
        }
    }

    private static VBox toastLayer(Scene scene) {
        return (VBox) host(scene).getProperties().get(TOASTS_KEY);
    }

    // ---------------------------------------------------------------- dialogs

    /** Shows a full-window layer (the sign-in loading screen). */
    public static Node showLayer(Scene scene, Node layer) {
        return addDialogLayer(scene, layer);
    }

    /** Shows a card in the middle of the window on a dimmed background. */
    public static Node showDialog(Scene scene, Node card) {
        StackPane scrim = new StackPane(card);
        scrim.getStyleClass().add("dialog-scrim");
        scrim.getProperties().put(CARD_KEY, Boolean.TRUE);
        scrim.setAlignment(Pos.CENTER);
        scrim.getStylesheets().add(OverlayHost.class.getResource(THEME).toExternalForm());

        card.setScaleX(0.92);
        card.setScaleY(0.92);

        ScaleTransition pop = new ScaleTransition(Duration.millis(260), card);
        pop.setToX(1);
        pop.setToY(1);
        pop.play();

        return addDialogLayer(scene, scrim);
    }

    /**
     * Closes the top-most pop-up card (not the sign-in loading screen).
     * Returns true if one was closed; used by the Esc key.
     */
    public static boolean closeTopDialog(Scene scene) {
        if (scene == null || !(scene.getRoot() instanceof StackPane host)
                || !host.getStyleClass().contains(HOST_STYLE)) {
            return false;
        }

        for (int i = host.getChildren().size() - 1; i >= 0; i--) {
            Node node = host.getChildren().get(i);

            if (node.getProperties().containsKey(CARD_KEY)) {
                node.getProperties().remove(CARD_KEY);   // already closing: do not close twice
                hide(node);
                return true;
            }
        }

        return false;
    }

    /** Fades a dialog or layer out and removes it. */
    public static void hide(Node layer) {
        if (layer == null) {
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(180), layer);
        fade.setToValue(0);
        fade.setOnFinished(event -> remove(layer));
        fade.play();
    }

    /** Removes a dialog or layer immediately. */
    public static void remove(Node layer) {
        if (layer != null && layer.getParent() instanceof Pane parent) {
            parent.getChildren().remove(layer);
        }
    }

    private static Node addDialogLayer(Scene scene, Node layer) {
        StackPane host = host(scene);

        layer.getProperties().put(DIALOG_KEY, Boolean.TRUE);
        layer.setOpacity(0);

        // Just below the toast layer, which is always last.
        host.getChildren().add(host.getChildren().size() - 1, layer);

        FadeTransition fade = new FadeTransition(Duration.millis(220), layer);
        fade.setToValue(1);
        fade.play();

        return layer;
    }
}
