package ph.edu.teleconsultation.system.portal.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.Feedback;
import ph.edu.teleconsultation.system.portal.service.FeedbackService;

/** The "How was your visit?" card: stars, a few quick tags and an optional comment. */
public class FeedbackFormController {

    private static final String[] TAGS = {
            "Doctor listened", "Clear advice", "Short wait",
            "Friendly staff", "Connection problems", "Felt rushed"
    };
    private static final String[] HINTS = { "Tap a star to rate", "Poor", "Fair", "Good", "Very good", "Excellent" };

    @FXML private Label doctorLabel;
    @FXML private Label visitLabel;
    @FXML private Label starHintLabel;
    @FXML private Label counterLabel;
    @FXML private Label errorLabel;
    @FXML private Label noteLabel;
    @FXML private HBox starBox;
    @FXML private FlowPane tagChips;
    @FXML private TextArea commentArea;
    @FXML private Button sendButton;

    private final List<Button> stars = new ArrayList<>();
    private int rating;
    private Consumer<FeedbackDialog.Choice> onSend;
    private Runnable onClose;

    void configure(Appointment appointment, Feedback existing,
                   Consumer<FeedbackDialog.Choice> onSend, Runnable onClose) {
        this.onSend = onSend;
        this.onClose = onClose;

        doctorLabel.setText(appointment.getDoctorName());
        visitLabel.setText("Visit on " + appointment.getDate() + " at " + appointment.getTime()
                + " \u00B7 " + appointment.getCategory());

        for (int i = 1; i <= 5; i++) {
            final int value = i;
            Button star = new Button("\u2605");
            star.getStyleClass().add("fb-star");
            star.setOnAction(e -> setRating(value));
            stars.add(star);
            starBox.getChildren().add(star);
        }

        for (String tag : TAGS) {
            ToggleButton chip = new ToggleButton(tag);
            chip.getStyleClass().add("visit-chip");
            chip.setUserData(tag);
            tagChips.getChildren().add(chip);
        }

        commentArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= FeedbackService.MAX_COMMENT_LENGTH ? change : null));
        commentArea.textProperty().addListener((obs, was, now) -> refreshCounter());

        if (existing != null) {
            setRating(existing.getRating());
            for (var node : tagChips.getChildren()) {
                ToggleButton chip = (ToggleButton) node;
                chip.setSelected(existing.getTags().contains((String) chip.getUserData()));
            }
            commentArea.setText(existing.getComment());
            sendButton.setText("Save changes");
            noteLabel.setText("You can change your feedback for " + FeedbackService.EDIT_WINDOW_HOURS
                    + " hours after sending it.");
        } else {
            noteLabel.setText("You can change your feedback for " + FeedbackService.EDIT_WINDOW_HOURS
                    + " hours after sending it.");
            setRating(0);
        }
        refreshCounter();
    }

    private void setRating(int value) {
        rating = value;
        for (int i = 0; i < stars.size(); i++) {
            var classes = stars.get(i).getStyleClass();
            classes.remove("fb-star-on");
            if (i < value) {
                classes.add("fb-star-on");
            }
        }
        starHintLabel.setText(HINTS[value]);
        showError(null);
    }

    private void refreshCounter() {
        counterLabel.setText(commentArea.getText().length() + "/" + FeedbackService.MAX_COMMENT_LENGTH);
    }

    private void showError(String problem) {
        boolean has = problem != null;
        errorLabel.setText(has ? problem : "");
        errorLabel.setVisible(has);
        errorLabel.setManaged(has);
    }

    @FXML
    private void handleSend() {
        if (rating < 1) {
            showError("Please tap a star to rate your visit.");
            return;
        }

        List<String> picked = new ArrayList<>();
        for (var node : tagChips.getChildren()) {
            ToggleButton chip = (ToggleButton) node;
            if (chip.isSelected()) {
                picked.add((String) chip.getUserData());
            }
        }

        onSend.accept(new FeedbackDialog.Choice(rating, picked, commentArea.getText()));
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    void focusDefault() {
        sendButton.requestFocus();
    }

    private void close() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
