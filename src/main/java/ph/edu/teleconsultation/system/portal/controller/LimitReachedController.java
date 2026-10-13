package ph.edu.teleconsultation.system.portal.controller;

import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import ph.edu.teleconsultation.system.portal.model.Appointment;

/** The "limit reached" card: shows the filled dots and the appointments that are using them up. */
public class LimitReachedController {

    @FXML private Label subtitleLabel;
    @FXML private Label countLabel;
    @FXML private HBox pips;
    @FXML private VBox activeList;
    @FXML private Button okButton;

    private Runnable onClose;

    void configure(List<Appointment> active, int max, Runnable onClose) {
        this.onClose = onClose;

        subtitleLabel.setText("You can't book more than " + max + " appointments at a time");
        countLabel.setText(active.size() + " of " + max + " active appointments");

        for (int i = 0; i < max; i++) {
            StackPane dot = new StackPane();
            dot.getStyleClass().add("limit-pip");
            if (i < active.size()) {
                dot.getStyleClass().add("limit-pip-full");
            }
            pips.getChildren().add(dot);
        }

        for (Appointment a : active) {
            Label who = new Label(a.getDoctorName());
            who.getStyleClass().add("visit-node-value");
            Label when = new Label(a.getDate() + " \u00B7 " + a.getTime());
            when.getStyleClass().add("visit-node-sub");
            VBox text = new VBox(2, who, when);
            HBox.setHgrow(text, Priority.ALWAYS);

            String status = a.getStatus().replace('_', ' ').toLowerCase();
            Label badge = new Label(Character.toUpperCase(status.charAt(0)) + status.substring(1));
            badge.getStyleClass().add("limit-badge");

            HBox row = new HBox(10, text, badge);
            row.getStyleClass().add("limit-row");
            row.setPadding(new Insets(10, 14, 10, 14));
            activeList.getChildren().add(row);
        }
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
