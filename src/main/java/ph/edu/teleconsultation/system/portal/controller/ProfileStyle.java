package ph.edu.teleconsultation.system.portal.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;

import ph.edu.teleconsultation.system.portal.model.ProfileTheme;

/**
 * Paints a person's chosen colour theme and photo onto a profile header.
 *
 * It works from the avatar's initials label: the label's parent is the round
 * avatar box and that box's parent is the header card, which is how every
 * profile page is laid out. Pages without a custom look keep their style-sheet
 * colours.
 */
final class ProfileStyle {

    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;

    private static final String PHOTO_KEY = "profile-photo";

    private ProfileStyle() {
    }

    /** Applies the theme and photo; a null photo shows the initials again. */
    static void apply(Label avatarLabel, String themeKey, byte[] photo) {

        if (avatarLabel == null || !(avatarLabel.getParent() instanceof Pane avatarBox)) {
            return;
        }

        ProfileTheme theme = ProfileTheme.byKey(themeKey);
        boolean isDefault = ProfileTheme.DEFAULT_KEY.equals(theme.key());

        // header colour + initials colour (the default keeps the style sheet's blue)
        if (avatarBox.getParent() instanceof Region header) {
            header.setStyle(isDefault
                    ? ""
                    : "-fx-background-color: " + theme.gradientCss() + ";");
        }

        avatarLabel.setStyle(isDefault ? "" : "-fx-text-fill: " + theme.text() + ";");

        // photo (replaces the initials)
        avatarBox.getChildren().removeIf(node -> node.getProperties().containsKey(PHOTO_KEY));

        double size = Math.max(60,
                (avatarBox.getPrefWidth() > 0 ? avatarBox.getPrefWidth() : 112) - 14);

        Node view = photo == null ? null : photoView(photo, size);

        if (view != null) {
            view.getProperties().put(PHOTO_KEY, Boolean.TRUE);
            avatarBox.getChildren().add(0, view);
        }

        avatarLabel.setVisible(view == null);
        avatarLabel.setManaged(view == null);
    }

    /** Reads and checks a photo file; the message of the exception is meant for the person. */
    static byte[] readPhoto(File file) throws IOException {

        if (file.length() > MAX_PHOTO_BYTES) {
            throw new IOException("That photo is larger than 5 MB. Please choose a smaller one.");
        }

        byte[] data = Files.readAllBytes(file.toPath());
        Image check = new Image(new ByteArrayInputStream(data));

        if (check.isError() || check.getWidth() <= 0 || check.getHeight() <= 0) {
            throw new IOException("That file could not be read as a picture. Use a PNG, JPG or GIF.");
        }

        return data;
    }

    /** A centre-cropped, circular picture. */
    private static Node photoView(byte[] data, double size) {

        Image image = new Image(new ByteArrayInputStream(data));

        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }

        double side = Math.min(image.getWidth(), image.getHeight());

        ImageView view = new ImageView(image);
        view.setViewport(new Rectangle2D(
                (image.getWidth() - side) / 2,
                (image.getHeight() - side) / 2,
                side,
                side));
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.setClip(new Circle(size / 2, size / 2, size / 2));

        return view;
    }
}
