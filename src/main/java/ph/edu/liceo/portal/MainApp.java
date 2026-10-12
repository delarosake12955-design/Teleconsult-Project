package ph.edu.liceo.portal;

import java.time.LocalDate;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.AccountStatus;
import ph.edu.liceo.portal.model.Admin;
import ph.edu.liceo.portal.model.ConsultationRequest;
import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.MedicalRecord;
import ph.edu.liceo.portal.model.Nurse;
import ph.edu.liceo.portal.model.Patient;
import ph.edu.liceo.portal.model.Secretary;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.model.VitalSigns;

public class MainApp extends Application {

    private static Stage stage;

    public static final double WIDTH = 1200;
    public static final double HEIGHT = 750;

    @Override
    public void start(Stage primaryStage) throws Exception {

        stage = primaryStage;

        stage.setTitle(
                "TeleConsult - Medical Consultation System"
        );

        // Start at the designed size, but never larger than the usable screen,
        // so the window always fits and keeps that size while navigating.
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        stage.setWidth(Math.min(WIDTH, screen.getWidth()));
        stage.setHeight(Math.min(HEIGHT, screen.getHeight()));

        stage.setMinWidth(Math.min(1000, screen.getWidth()));
        stage.setMinHeight(Math.min(650, screen.getHeight()));

        createDemoAccounts();

        new ph.edu.liceo.portal.service.NotificationService().seedWelcome();

        // Notifications for the signed-in user now pop up on screen.
        ph.edu.liceo.portal.controller.NotificationToast.install();

        showLoadingScreen();

        stage.show();
    }

    private void createDemoAccounts() {

        boolean adminExists = false;
        boolean doctorExists = false;
        boolean nurseExists = false;
        boolean secretaryExists = false;
        boolean patientExists = false;

        for (User user : DataStore.users) {
            if (user instanceof Admin) adminExists = true;
            if (user instanceof Doctor) doctorExists = true;
            if (user instanceof Nurse) nurseExists = true;
            if (user instanceof Secretary) secretaryExists = true;
            if (user instanceof Patient) patientExists = true;
        }

        if (!adminExists) {
            Admin admin = new Admin(
                    DataStore.getNextUserId(),
                    "System Administrator",
                    "admin@teleconsult.com",
                    "admin123"
            );
            DataStore.users.add(admin);
        }

        if (!doctorExists) {
            Doctor doctorOne = new Doctor(
                    DataStore.getNextUserId(),
                    "Dr. Juan Dela Cruz",
                    "doctor@teleconsult.com",
                    "doctor123",
                    "MD-2026-001",
                    "General Medicine"
            );
            doctorOne.setStatus(AccountStatus.ACTIVE);
            DataStore.users.add(doctorOne);

            Doctor doctorTwo = new Doctor(
                    DataStore.getNextUserId(),
                    "Dr. Maria Santos",
                    "maria@teleconsult.com",
                    "doctor123",
                    "MD-2026-002",
                    "Cardiology"
            );
            doctorTwo.setStatus(AccountStatus.ACTIVE);
            DataStore.users.add(doctorTwo);
        }

        if (!nurseExists) {
            Nurse nurse = new Nurse(
                    DataStore.getNextUserId(),
                    "Nurse Ana Reyes",
                    "nurse@teleconsult.com",
                    "nurse123",
                    "RN-2026-001",
                    "Clinical Services"
            );
            nurse.setStatus(AccountStatus.ACTIVE);
            DataStore.users.add(nurse);
        }

        if (!secretaryExists) {
            Secretary secretary = new Secretary(
                    DataStore.getNextUserId(),
                    "Secretary Carla Garcia",
                    "secretary@teleconsult.com",
                    "secretary123",
                    "Medical Administration"
            );
            secretary.setStatus(AccountStatus.ACTIVE);
            DataStore.users.add(secretary);
        }

        if (!patientExists) {
            Patient patient = new Patient(
                    DataStore.getNextUserId(),
                    "Demo Patient",
                    "patient@teleconsult.com",
                    "patient123",
                    "2004-01-15",
                    "09171234567",
                    "Cagayan de Oro"
            );
            DataStore.users.add(patient);
        }

        // Two extra patients so the Senior / PWD priority can be shown in a demo.
        if (DataStore.findUserByEmail("senior@teleconsult.com") == null) {
            Patient senior = new Patient(
                    DataStore.getNextUserId(),
                    "Demo Senior",
                    "senior@teleconsult.com",
                    "senior123",
                    LocalDate.now().minusYears(68).toString(),
                    "09171234568",
                    "Cagayan de Oro"
            );
            DataStore.users.add(senior);
        }

        if (DataStore.findUserByEmail("pwd@teleconsult.com") == null) {
            Patient pwdPatient = new Patient(
                    DataStore.getNextUserId(),
                    "Demo PWD",
                    "pwd@teleconsult.com",
                    "pwd12345",
                    "1990-06-20",
                    "09171234569",
                    "Cagayan de Oro"
            );
            pwdPatient.setPwd(true);
            pwdPatient.setPwdIdNumber("PWD-2026-0001");
            DataStore.users.add(pwdPatient);
        }

        createDemoHistory();
    }

    /** Adds one completed consultation so record and history pages are not empty. */
    private void createDemoHistory() {

        if (!DataStore.medicalRecords.isEmpty()
                || !DataStore.consultationRequests.isEmpty()) {
            return;
        }

        ConsultationRequest request = new ConsultationRequest(
                DataStore.getNextConsultationId(),
                "Demo Patient",
                "Dr. Juan Dela Cruz",
                "Persistent cough and mild fever for three days.",
                LocalDate.now().minusDays(7).toString()
        );
        request.setStatus("COMPLETED");
        request.setNotes("Acute upper respiratory infection. Rest, fluids and paracetamol every 6 hours as needed.");
        DataStore.consultationRequests.add(request);

        DataStore.medicalRecords.add(new MedicalRecord(
                DataStore.getNextMedicalRecordId(),
                "Demo Patient",
                "Dr. Juan Dela Cruz",
                LocalDate.now().minusDays(7).toString(),
                "Acute upper respiratory infection",
                request.getNotes(),
                new VitalSigns(37.8, 88, 118, 76, 18)
        ));
    }

    private void showLoadingScreen() {

        StackPane root = new StackPane();

        root.getStyleClass().add(
                "loading-overlay"
        );

        StackPane glassLayers = new StackPane();

        glassLayers.setPrefWidth(430);
        glassLayers.setPrefHeight(430);

        glassLayers.setMaxWidth(430);
        glassLayers.setMaxHeight(430);

        StackPane layerOne = new StackPane();

        layerOne.setPrefWidth(300);
        layerOne.setPrefHeight(300);

        layerOne.setMaxWidth(300);
        layerOne.setMaxHeight(300);

        layerOne.getStyleClass().add(
                "loading-glass-layer-one"
        );

        layerOne.setTranslateX(-45);
        layerOne.setTranslateY(-30);

        StackPane layerTwo = new StackPane();

        layerTwo.setPrefWidth(300);
        layerTwo.setPrefHeight(300);

        layerTwo.setMaxWidth(300);
        layerTwo.setMaxHeight(300);

        layerTwo.getStyleClass().add(
                "loading-glass-layer-two"
        );

        layerTwo.setTranslateX(40);
        layerTwo.setTranslateY(35);

        StackPane layerThree = new StackPane();

        layerThree.setPrefWidth(300);
        layerThree.setPrefHeight(300);

        layerThree.setMaxWidth(300);
        layerThree.setMaxHeight(300);

        layerThree.getStyleClass().add(
                "loading-glass-layer-three"
        );

        layerThree.setTranslateX(0);
        layerThree.setTranslateY(0);

        glassLayers.getChildren().addAll(
                layerOne,
                layerTwo,
                layerThree
        );

        VBox loadingBox = new VBox();

        loadingBox.setAlignment(
                javafx.geometry.Pos.CENTER
        );

        loadingBox.setSpacing(14);

        loadingBox.getStyleClass().add(
                "loading-box"
        );

        StackPane logo = new StackPane();

        logo.getStyleClass().add(
                "loading-logo"
        );

        Label plus = new Label("+");

        plus.getStyleClass().add(
                "loading-plus"
        );

        logo.getChildren().add(plus);

        Label title = new Label(
                "TeleConsult"
        );

        title.getStyleClass().add(
                "loading-title"
        );

        Label subtitle = new Label(
                "MEDICAL SYSTEM"
        );

        subtitle.getStyleClass().add(
                "loading-subtitle"
        );

        ProgressIndicator progress =
                new ProgressIndicator();

        progress.setProgress(-1);

        progress.getStyleClass().add(
                "loading-progress"
        );

        Label loadingText = new Label(
                "Loading TeleConsult..."
        );

        loadingText.getStyleClass().add(
                "loading-text"
        );

        loadingBox.getChildren().addAll(
                logo,
                title,
                subtitle,
                progress,
                loadingText
        );

        root.getChildren().addAll(
                glassLayers,
                loadingBox
        );

        Scene scene = new Scene(
                root,
                WIDTH,
                HEIGHT
        );

        scene.getStylesheets().add(
                MainApp.class
                        .getResource("homepage.css")
                        .toExternalForm()
        );

        stage.setScene(scene);

        setupFullscreen();

        TranslateTransition moveLayerOne =
                new TranslateTransition(
                        Duration.seconds(3.5),
                        layerOne
                );

        moveLayerOne.setFromX(-45);
        moveLayerOne.setFromY(-30);

        moveLayerOne.setToX(-20);
        moveLayerOne.setToY(-45);

        moveLayerOne.setAutoReverse(true);

        moveLayerOne.setCycleCount(
                TranslateTransition.INDEFINITE
        );

        TranslateTransition moveLayerTwo =
                new TranslateTransition(
                        Duration.seconds(4),
                        layerTwo
                );

        moveLayerTwo.setFromX(40);
        moveLayerTwo.setFromY(35);

        moveLayerTwo.setToX(15);
        moveLayerTwo.setToY(50);

        moveLayerTwo.setAutoReverse(true);

        moveLayerTwo.setCycleCount(
                TranslateTransition.INDEFINITE
        );

        RotateTransition rotateLayers =
                new RotateTransition(
                        Duration.seconds(12),
                        glassLayers
                );

        rotateLayers.setFromAngle(0);
        rotateLayers.setToAngle(360);

        rotateLayers.setCycleCount(
                RotateTransition.INDEFINITE
        );

        moveLayerOne.play();
        moveLayerTwo.play();
        rotateLayers.play();

        root.setOpacity(0);

        FadeTransition loadingFade =
                new FadeTransition(
                        Duration.millis(700),
                        root
                );

        loadingFade.setFromValue(0);
        loadingFade.setToValue(1);

        loadingFade.play();

        PauseTransition delay =
                new PauseTransition(
                        Duration.seconds(1.8)
                );

        delay.setOnFinished(event -> {

            showHomepageWithFade();
        });

        delay.play();
    }

    private void showHomepageWithFade() {

        try {

            Parent root = FXMLLoader.load(
                    MainApp.class.getResource(
                            "homepage.fxml"
                    )
            );

            root.setOpacity(0);

            if (stage.getScene() == null) {

                Scene scene = new Scene(
                        root,
                        WIDTH,
                        HEIGHT
                );

                stage.setScene(scene);

            } else {

                stage.getScene().getStylesheets().clear();

                ph.edu.liceo.portal.controller.OverlayHost.setPage(stage.getScene(), root);
            }

            setupFullscreen();

            FadeTransition fade =
                    new FadeTransition(
                            Duration.millis(900),
                            root
                    );

            fade.setFromValue(0);
            fade.setToValue(1);

            fade.play();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /**
     * Puts a page into the existing window without changing the window's
     * size, position or fullscreen state (creating a new Scene would make the
     * window jump to that page's preferred size and drop the F11 shortcut).
     */
    public static void showRoot(Parent root) {

        if (stage == null || root == null) {

            return;
        }

        Scene scene = stage.getScene();

        if (scene == null) {

            scene = new Scene(root, WIDTH, HEIGHT);

            stage.setScene(scene);

        } else {

            // Every page brings its own stylesheet, so drop the loading
            // screen's scene-level one instead of letting it leak across pages.
            scene.getStylesheets().clear();

            ph.edu.liceo.portal.controller.OverlayHost.setPage(scene, root);
        }

        setupFullscreen();
    }

    public static void showPage(String page) {

        try {

            Parent root = FXMLLoader.load(
                    MainApp.class.getResource(page)
            );

            Scene scene = stage.getScene();

            if (scene == null) {

                scene = new Scene(
                        root,
                        WIDTH,
                        HEIGHT
                );

                stage.setScene(scene);

            } else {

                scene.getStylesheets().clear();

                ph.edu.liceo.portal.controller.OverlayHost.setPage(scene, root);
            }

            setupFullscreen();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void showHomepage() {

        showPage("homepage.fxml");
    }

    private static void setupFullscreen() {

        if (stage == null ||
                stage.getScene() == null) {

            return;
        }

        // Esc asks "Do you want to log out?" (or closes an open pop-up card).
        ph.edu.liceo.portal.controller.LogoutConfirm.install(stage);

        stage.getScene().setOnKeyPressed(event -> {

            if (event.getCode() == KeyCode.F11) {

                stage.setFullScreen(
                        !stage.isFullScreen()
                );
            }
        });
    }

    public static Stage getStage() {

        return stage;
    }

    public static void main(String[] args) {

        launch(args);
    }
}