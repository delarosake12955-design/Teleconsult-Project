package ph.edu.teleconsultation.system.portal.controller;

import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import ph.edu.teleconsultation.system.portal.model.Appointment;

/** The "Visit completed" card the doctor sees right after pressing Mark Completed. */
public class VisitCompletedController {

    @FXML private StackPane badge;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label noteLabel;
    @FXML private VBox rows;
    @FXML private Button okButton;

    private Runnable onClose;

    void configure(List<Appointment> done, Runnable onClose) {
        this.onClose = onClose;

        boolean one = done.size() == 1;
        titleLabel.setText(one ? "Visit completed" : done.size() + " visits completed");
        subtitleLabel.setText(one
                ? done.get(0).getPatientName() + " was marked as done."
                : "All selected visits were marked as done.");
        noteLabel.setText(one
                ? "The patient was notified and can now rate this visit."
                : "The patients were notified and can now rate their visits.");

        for (Appointment a : done) {
            Label who = new Label(a.getPatientName());
            who.getStyleClass().add("visit-node-value");
            Label when = new Label(a.getDate() + " \u00B7 " + a.getTime() + " \u00B7 " + a.getCategory());
            when.getStyleClass().add("visit-node-sub");
            VBox text = new VBox(2, who, when);

            Label tick = new Label("Completed \u2713");
            tick.getStyleClass().add("done-badge-small");

            HBox row = new HBox(10, text, tick);
            row.getStyleClass().add("done-row");
            HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);
            rows.getChildren().add(row);
        }

        // The check pops in so the doctor can see it worked.
        badge.setScaleX(0.3);
        badge.setScaleY(0.3);
        badge.setOpacity(0);

        ScaleTransition grow = new ScaleTransition(Duration.millis(380), badge);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(260), badge);
        fade.setToValue(1);

        new ParallelTransition(grow, fade).play();
    }

    @FXML
    private void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
    }

    void focusDefault() {
        okButton.requestFocus();
    }
}
