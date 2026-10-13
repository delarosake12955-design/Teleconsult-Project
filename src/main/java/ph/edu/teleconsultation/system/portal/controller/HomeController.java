package ph.edu.teleconsultation.system.portal.controller;

import java.io.IOException;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class HomeController {

    @FXML
    private VBox contentWrapper;

    @FXML
    private StackPane medicalCard;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private Node servicesSection;

    @FXML
    private Node aboutSection;

    @FXML
    private Node contactSection;

    @FXML
    private void initialize() {

        startEntranceAnimation();
        startMedicalCardAnimation();
    }

    @FXML
    private void handleHome(ActionEvent event) {

        smoothScrollTo(0);
    }

    @FXML
    private void handleAbout() {

        scrollToSection(aboutSection, 0.55);
    }

    @FXML
    private void handleServices() {

        scrollToSection(servicesSection, 0.35);
    }

    @FXML
    private void handleContact() {

        scrollToSection(contactSection, 1.0);
    }

    @FXML
    private void handleLogin(ActionEvent event) {

        openPage(event, "login.fxml");
    }

    @FXML
    private void handleRegister(ActionEvent event) {

        openPage(event, "register.fxml");
    }

    /**
     * Scrolls so the given section sits at the top of the view, wherever the
     * layout puts it. Falls back to a fixed position if sizes are not known yet.
     */
    private void scrollToSection(Node section, double fallback) {

        if (contentScrollPane == null || contentWrapper == null || section == null) {

            smoothScrollTo(fallback);
            return;
        }

        double scrollable = contentWrapper.getBoundsInLocal().getHeight()
                - contentScrollPane.getViewportBounds().getHeight();

        if (scrollable <= 0) {

            smoothScrollTo(fallback);
            return;
        }

        Bounds bounds = contentWrapper.sceneToLocal(
                section.localToScene(section.getBoundsInLocal())
        );

        double target = (bounds.getMinY() - 12) / scrollable;

        smoothScrollTo(Math.max(0, Math.min(1, target)));
    }

    private void smoothScrollTo(double target) {

        if (contentScrollPane == null) {
            return;
        }

        Timeline timeline = new Timeline();

        KeyValue keyValue = new KeyValue(
                contentScrollPane.vvalueProperty(),
                target,
                Interpolator.EASE_BOTH
        );

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(250),
                keyValue
        );

        timeline.getKeyFrames().add(keyFrame);

        timeline.play();
    }

    private void startEntranceAnimation() {

        if (contentWrapper == null) {
            return;
        }

        contentWrapper.setOpacity(0);
        contentWrapper.setTranslateY(25);

        FadeTransition fade = new FadeTransition(
                Duration.millis(900),
                contentWrapper
        );

        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(
                Duration.millis(900),
                contentWrapper
        );

        slide.setFromY(25);
        slide.setToY(0);

        fade.play();
        slide.play();
    }

    private void startMedicalCardAnimation() {

        if (medicalCard == null) {
            return;
        }

        TranslateTransition animation = new TranslateTransition(
                Duration.seconds(3),
                medicalCard
        );

        animation.setFromY(0);
        animation.setToY(-8);
        animation.setAutoReverse(true);
        animation.setCycleCount(TranslateTransition.INDEFINITE);

        animation.play();
    }

    private void openPage(ActionEvent event, String page) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/ph/edu/teleconsultation/system/portal/" + page
                    )
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            ph.edu.teleconsultation.system.portal.MainApp.showRoot(root);
            stage.show();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}