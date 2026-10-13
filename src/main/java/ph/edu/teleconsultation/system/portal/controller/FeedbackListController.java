package ph.edu.teleconsultation.system.portal.controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import ph.edu.teleconsultation.system.portal.model.Feedback;
import ph.edu.teleconsultation.system.portal.service.FeedbackService;

/** The doctor's (or admin's) view of patient feedback: the average on top, then each comment. */
public class FeedbackListController {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label averageLabel;
    @FXML private Label averageStarsLabel;
    @FXML private Label countLabel;
    @FXML private VBox rows;

    private Runnable onClose;

    void configure(String title, String subtitle, List<Feedback> items, boolean showDoctor, Runnable onClose) {
        this.onClose = onClose;

        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);

        double average = FeedbackService.average(items);
        averageLabel.setText(items.isEmpty() ? "\u2014" : String.format(Locale.ENGLISH, "%.1f", average));
        averageStarsLabel.setText(Feedback.stars((int) Math.round(average)));
        countLabel.setText(items.size() == 1 ? "1 review" : items.size() + " reviews");

        if (items.isEmpty()) {
            Label none = new Label("No feedback yet. It appears here after patients rate a completed visit.");
            none.getStyleClass().add("visit-empty");
            none.setWrapText(true);
            rows.getChildren().add(none);
            return;
        }

        for (Feedback f : items) {
            Label stars = new Label(f.stars());
            stars.getStyleClass().add("fb-stars-small");

            String who = f.getPatientName() + (showDoctor ? " \u2192 " + f.getDoctorName() : "");
            Label name = new Label(who);
            name.getStyleClass().add("visit-node-value");
            Label when = new Label("Visit " + f.getAppointmentDate() + " \u00B7 sent "
                    + f.getCreatedAt().format(WHEN));
            when.getStyleClass().add("visit-node-sub");
            VBox left = new VBox(2, name, when);
            HBox.setHgrow(left, Priority.ALWAYS);

            HBox top = new HBox(10, left, stars);

            VBox card = new VBox(8, top);
            card.getStyleClass().add("fb-row");
            if (f.getRating() <= FeedbackService.LOW_RATING) {
                card.getStyleClass().add("fb-row-low");
            }

            if (!f.getTags().isEmpty()) {
                Label tags = new Label(String.join(" \u00B7 ", f.getTags()));
                tags.getStyleClass().add("fb-tags");
                tags.setWrapText(true);
                card.getChildren().add(tags);
            }
            if (!f.getComment().isEmpty()) {
                Label comment = new Label(f.getComment());
                comment.getStyleClass().add("fb-comment");
                comment.setWrapText(true);
                card.getChildren().add(comment);
            }
            rows.getChildren().add(card);
        }
    }

    @FXML
    private void handleClose() {
        if (onClose != null) {
            onClose.run();
        }
    }
}
