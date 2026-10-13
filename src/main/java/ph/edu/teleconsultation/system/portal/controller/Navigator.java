package ph.edu.teleconsultation.system.portal.controller;

import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import ph.edu.teleconsultation.system.portal.MainApp;
import ph.edu.teleconsultation.system.portal.model.Admin;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.User;

/**
 * Central, role-aware page navigation.
 *
 * Pages opened from inside a role dashboard are embedded into the dashboard's
 * "contentHost" so the sidebar always stays visible and consistent. When no
 * dashboard shell is present the page replaces the scene root instead.
 */
public final class Navigator {

    private static final String BASE = "/ph/edu/teleconsultation/system/portal/";

    private Navigator() {
    }

    public static String roleOf(User user) {
        if (user instanceof Admin) return "admin";
        if (user instanceof Doctor) return "doctor";
        if (user instanceof Nurse) return "nurse";
        if (user instanceof Secretary) return "secretary";
        if (user instanceof Patient) return "patient";
        return "patient";
    }

    /** Resolves a logical section (e.g. "records") to the FXML for the logged in role. */
    public static String pageFor(String section) {
        User user = LoginController.getLoggedInUser();
        String role = roleOf(user);

        return switch (section) {
            case "dashboard" -> role + "-dashboard.fxml";
            case "notifications" -> "notifications.fxml";
            case "appointments" -> role + "-appointments.fxml";
            case "doctors" -> switch (role) {
                case "doctor" -> "doctor-patients.fxml";
                case "nurse" -> "nurse-patients.fxml";
                default -> role + "-doctors.fxml";
            };
            case "patients" -> switch (role) {
                case "patient" -> "patient-medical-records.fxml";
                default -> role + "-patients.fxml";
            };
            case "consultation" -> role + "-consultation.fxml";
            case "requests" -> switch (role) {
                case "patient" -> "patient-requests.fxml";
                case "admin", "secretary" -> role + "-verification-requests.fxml";
                default -> role + "-consultation.fxml";
            };
            case "records" -> role + "-medical-records.fxml";
            case "profile" -> role.equals("doctor")
                    ? "doctor-profile-page.fxml"
                    : role + "-profile.fxml";
            case "verification" -> role.equals("admin") || role.equals("secretary")
                    ? role + "-verification-requests.fxml"
                    : role + "-dashboard.fxml";
            case "users" -> switch (role) {
                case "admin" -> "admin-users.fxml";
                case "secretary" -> "secretary-patients.fxml";
                default -> role + "-dashboard.fxml";
            };
            case "schedule" -> role.equals("doctor")
                    ? "doctor-schedule.fxml"
                    : role + "-appointments.fxml";
            default -> role + "-dashboard.fxml";
        };
    }

    /** Opens a logical section for the logged in user. */
    public static void openSection(Node source, String section) {
        open(source, pageFor(section));
    }

    /** Opens an FXML page, embedding it in the dashboard shell when one exists. */
    public static void open(Node source, String page) {
        if (source == null || source.getScene() == null) {
            MainApp.showPage(page);
            return;
        }

        Scene scene = source.getScene();

        try {
            URL resource = Navigator.class.getResource(BASE + page);

            if (resource == null) {
                System.err.println("FXML file not found: " + page);
                return;
            }

            Node host = scene.lookup("#contentHost");
            if (host == null) {
                host = scene.lookup(".content-host");
            }
            boolean isDashboard = page.endsWith("-dashboard.fxml");

            Parent loaded = FXMLLoader.load(resource);

            if (host instanceof StackPane contentHost && !isDashboard) {
                if (loaded instanceof BorderPane borderPane) {
                    // Inner pages ship with their own sidebar; the dashboard
                    // shell already provides one, so drop the duplicate.
                    borderPane.setLeft(null);
                    borderPane.setPrefWidth(Double.MAX_VALUE);
                    borderPane.setPrefHeight(Double.MAX_VALUE);
                }
                contentHost.getChildren().setAll(loaded);
                return;
            }

            OverlayHost.setPage(scene, loaded);

        } catch (Exception e) {
            System.err.println("Failed to open FXML page: " + page);
            e.printStackTrace();
        }
    }

    public static void logout(Node source) {
        LoginController.clearLoggedInUser();
        NotificationToast.clearAll();
        MainApp.showHomepage();
    }
}
