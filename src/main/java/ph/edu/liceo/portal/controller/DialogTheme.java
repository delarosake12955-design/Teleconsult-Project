package ph.edu.liceo.portal.controller;

import javafx.geometry.Insets;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/** Gives JavaFX dialogs and alerts the TeleConsult look. */
final class DialogTheme {

    private static final String THEME = "/ph/edu/liceo/portal/popup-theme.css";

    private DialogTheme() {
    }

    static void apply(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        var url = DialogTheme.class.getResource(THEME);
        if (url != null) {
            pane.getStylesheets().add(url.toExternalForm());
        }
        pane.getStyleClass().add("tc-dialog");
    }

    /** Roomier spacing and sensible column widths for a label + field form. */
    static void styleForm(GridPane grid) {
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(8, 6, 0, 6));

        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(150);

        ColumnConstraints fields = new ColumnConstraints();
        fields.setMinWidth(200);
        fields.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().setAll(labels, fields);
    }
}
