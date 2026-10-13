package ph.edu.teleconsultation.system.portal.controller;

import java.util.ArrayList;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** The sign-in loading screen: role badge, welcome line and a few "loading..." steps. */
public class LoadingController {

    private static final double STEP_MILLIS = 480;
    private static final double HOLD_MILLIS = 320;

    @FXML private StackPane glassLayers;
    @FXML private StackPane layerOne;
    @FXML private StackPane layerTwo;
    @FXML private Label roleLabel;
    @FXML private Label greetingLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar stepBar;

    private final List<Animation> background = new ArrayList<>();

    @FXML
    private void initialize() {
        TranslateTransition floatOne = new TranslateTransition(Duration.seconds(3.5), layerOne);
        floatOne.setFromX(-45);
        floatOne.setFromY(-30);
        floatOne.setToX(-20);
        floatOne.setToY(-45);
        floatOne.setAutoReverse(true);
        floatOne.setCycleCount(Animation.INDEFINITE);

        TranslateTransition floatTwo = new TranslateTransition(Duration.seconds(4), layerTwo);
        floatTwo.setFromX(40);
        floatTwo.setFromY(35);
        floatTwo.setToX(15);
        floatTwo.setToY(50);
        floatTwo.setAutoReverse(true);
        floatTwo.setCycleCount(Animation.INDEFINITE);

        RotateTransition spin = new RotateTransition(Duration.seconds(12), glassLayers);
        spin.setFromAngle(0);
        spin.setToAngle(360);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.setCycleCount(Animation.INDEFINITE);

        background.add(floatOne);
        background.add(floatTwo);
        background.add(spin);

        for (Animation animation : background) {
            animation.play();
        }
    }

    /**
     * Runs the loading sequence and calls {@code done} when it ends.
     *
     * @param portal  e.g. "DOCTOR PORTAL"
     * @param roleKey doctor / patient / nurse / secretary / admin (picks the badge colour)
     * @param steps   the status lines shown one after another
     */
    void play(String portal, String roleKey, String name, List<String> steps, Runnable done) {
        roleLabel.setText(portal);
        roleLabel.getStyleClass().add("loading-role-" + roleKey);
        greetingLabel.setText("Welcome, " + name);

        int count = steps.size();
        Timeline sequence = new Timeline();

        for (int i = 0; i < count; i++) {
            String text = steps.get(i);

            sequence.getKeyFrames().add(new KeyFrame(
                    Duration.millis(i * STEP_MILLIS),
                    event -> statusLabel.setText(text),
                    new KeyValue(stepBar.progressProperty(), i / (double) count)));
        }

        sequence.getKeyFrames().add(new KeyFrame(
                Duration.millis(count * STEP_MILLIS),
                event -> statusLabel.setText("Ready!"),
                new KeyValue(stepBar.progressProperty(), 1.0)));

        sequence.getKeyFrames().add(new KeyFrame(
                Duration.millis(count * STEP_MILLIS + HOLD_MILLIS)));

        sequence.setOnFinished(event -> {
            for (Animation animation : background) {
                animation.stop();
            }

            done.run();
        });

        sequence.play();
    }
}
