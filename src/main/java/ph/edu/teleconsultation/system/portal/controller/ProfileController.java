package ph.edu.teleconsultation.system.portal.controller;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.PatientType;
import ph.edu.teleconsultation.system.portal.model.ProfileTheme;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.User;
import ph.edu.teleconsultation.system.portal.service.UserService;

/**
 * Shared profile page for every role. Patients can edit contact details,
 * every other role edits name and email.
 */
public class ProfileController extends PageController {

    private static final Pattern EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE =
            Pattern.compile("^[0-9+\\-\\s]{7,15}$");

    @FXML private Label avatarLabel;
    @FXML private Label nameLabel;
    @FXML private Label roleLabel;
    @FXML private Label emailHeaderLabel;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField dateOfBirthField;
    @FXML private TextField addressField;
    @FXML private Label messageLabel;
    @FXML private Label statusLabel;
    @FXML private Label accountTypeLabel;
    /** Only on the patient profile page; null on the other roles' pages. */
    @FXML private Label patientIdLabel;
    @FXML private Button saveButton;

    // Patient type (only on the patient profile page; null on the other roles' pages).
    @FXML private Label patientTypeLabel;
    @FXML private CheckBox pwdCheckBox;
    @FXML private TextField pwdIdField;

    // Profile customization (only on the patient, doctor, nurse and secretary pages;
    // all of these are null on the administrator page).
    @FXML private Label bioHeaderLabel;
    @FXML private HBox themeSwatches;
    @FXML private Label themeNameLabel;
    @FXML private TextArea bioArea;
    @FXML private Label bioCounterLabel;
    @FXML private Label customMessageLabel;

    private static final int MAX_BIO = 140;

    private final UserService userService = new UserService();
    private User user;

    // What the customization card is previewing (saved only by "Save Customization").
    private boolean customizing;
    private String pendingTheme;
    private byte[] pendingPhoto;

    @FXML
    private void initialize() {
        user = LoginController.getLoggedInUser();

        if (user == null) {
            setMessage("No user is signed in.", false);
            saveButton.setDisable(true);
            return;
        }

        configureFieldsForRole();
        setupCustomization();
        loadProfile();
    }

    private void configureFieldsForRole() {
        boolean patient = user instanceof Patient;

        setEditable(phoneField, patient);
        setEditable(dateOfBirthField, patient);
        setEditable(addressField, patient);

        if (!patient) {
            phoneField.setPromptText("Not applicable for this account type");
            dateOfBirthField.setPromptText("Not applicable for this account type");
            addressField.setPromptText("Not applicable for this account type");
        } else {
            dateOfBirthField.setPromptText("YYYY-MM-DD");
        }
    }

    private void setEditable(TextField field, boolean editable) {
        field.setEditable(editable);
        field.setDisable(!editable);
    }

    private void loadProfile() {
        fullNameField.setText(user.getFullName());
        emailField.setText(user.getEmail());

        if (user instanceof Patient patient) {
            phoneField.setText(nullToEmpty(patient.getPhoneNumber()));
            dateOfBirthField.setText(nullToEmpty(patient.getDateOfBirth()));
            addressField.setText(nullToEmpty(patient.getAddress()));
            loadPatientType(patient);
        }

        refreshHeader();
        statusLabel.setText(user.getStatus().name());
        accountTypeLabel.setText(roleName());

        if (patientIdLabel != null && user instanceof Patient patient) {
            patientIdLabel.setText(patient.getPatientId());
        }
    }

    private void refreshHeader() {
        nameLabel.setText(user.getFullName());
        emailHeaderLabel.setText(user.getEmail());
        avatarLabel.setText(initials(user.getFullName()));
        roleLabel.setText(roleDescription());

        // Show the preview while the customization card is being used,
        // otherwise what has been saved.
        ProfileStyle.apply(avatarLabel,
                customizing ? pendingTheme : user.getThemeKey(),
                customizing ? pendingPhoto : user.getPhoto());
        showBio(customizing ? bioArea.getText() : user.getBio());
    }

    // ---------------------------------------------------------------- customization

    private void setupCustomization() {

        customizing = themeSwatches != null && bioArea != null;

        if (!customizing) {
            return;
        }

        pendingTheme = user.getThemeKey();
        pendingPhoto = user.getPhoto();

        for (ProfileTheme theme : ProfileTheme.all()) {

            javafx.scene.control.Button swatch = new javafx.scene.control.Button();
            swatch.getStyleClass().add("theme-swatch");
            swatch.setStyle("-fx-background-color: " + theme.gradientCss() + ";");
            swatch.setTooltip(new Tooltip(theme.name()));
            swatch.setUserData(theme.key());
            swatch.setOnAction(event -> {
                pendingTheme = theme.key();
                updateSwatches();
                refreshHeader();
            });

            themeSwatches.getChildren().add(swatch);
        }

        bioArea.setText(user.getBio());
        updateBioCounter();

        bioArea.textProperty().addListener((observable, old, text) -> {

            if (text.length() > MAX_BIO) {
                bioArea.setText(old);
                return;
            }

            updateBioCounter();
            showBio(text);
        });

        updateSwatches();
    }

    private void updateSwatches() {

        for (javafx.scene.Node node : themeSwatches.getChildren()) {

            node.getStyleClass().remove("theme-swatch-selected");

            if (pendingTheme.equals(node.getUserData())) {
                node.getStyleClass().add("theme-swatch-selected");
            }
        }

        themeNameLabel.setText(ProfileTheme.byKey(pendingTheme).name());
    }

    private void updateBioCounter() {
        bioCounterLabel.setText(bioArea.getText().length() + " / " + MAX_BIO);
    }

    private void showBio(String text) {

        if (bioHeaderLabel == null) {
            return;
        }

        boolean has = text != null && !text.isBlank();

        bioHeaderLabel.setText(has ? text.trim() : "");
        bioHeaderLabel.setVisible(has);
        bioHeaderLabel.setManaged(has);
    }

    @FXML
    private void handleChoosePhoto() {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a profile photo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Pictures", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));

        File file = chooser.showOpenDialog(avatarLabel.getScene().getWindow());

        if (file == null) {
            return;
        }

        try {
            pendingPhoto = ProfileStyle.readPhoto(file);
            refreshHeader();
            showCustomMessage("Photo ready. Press Save Customization to keep it.", true);

        } catch (IOException e) {
            showCustomMessage(e.getMessage(), false);
        }
    }

    @FXML
    private void handleRemovePhoto() {
        pendingPhoto = null;
        refreshHeader();
        showCustomMessage("Photo removed. Press Save Customization to keep this.", true);
    }

    @FXML
    private void handleSaveCustomization() {

        user.setThemeKey(pendingTheme);
        user.setPhoto(pendingPhoto);
        user.setBio(bioArea.getText());

        showCustomMessage("Your profile look has been saved.", true);
    }

    @FXML
    private void handleResetCustomization() {

        pendingTheme = ProfileTheme.DEFAULT_KEY;
        pendingPhoto = null;
        bioArea.setText("");

        updateSwatches();
        refreshHeader();
        showCustomMessage("Defaults restored. Press Save Customization to keep them.", true);
    }

    private void showCustomMessage(String text, boolean success) {

        customMessageLabel.setText(text);
        customMessageLabel.getStyleClass().removeAll("message-success", "message-error");
        customMessageLabel.getStyleClass().add(success ? "message-success" : "message-error");
    }

    @FXML
    private void handleSave() {
        if (user == null) {
            return;
        }

        String name = fullNameField.getText().trim();
        if (user instanceof Doctor) {
            name = Doctor.withTitle(name);
        }
        String email = emailField.getText().trim();

        if (name.isEmpty()) {
            setMessage("Full name is required.", false);
            return;
        }

        if (!EMAIL.matcher(email).matches()) {
            setMessage("Enter a valid email address.", false);
            return;
        }

        if (userService.nameTakenByOther(user, name)) {
            setMessage("Another account already uses that full name.", false);
            return;
        }

        if (userService.emailTakenByOther(user, email)) {
            setMessage("That email is already used by another account.", false);
            return;
        }

        if (user instanceof Patient patient) {
            String phone = phoneField.getText().trim();
            String dob = dateOfBirthField.getText().trim();
            String address = addressField.getText().trim();

            if (!phone.isEmpty() && !PHONE.matcher(phone).matches()) {
                setMessage("Phone number must contain 7 to 15 digits.", false);
                return;
            }

            if (!dob.isEmpty()) {
                try {
                    LocalDate parsed = LocalDate.parse(dob);
                    if (parsed.isAfter(LocalDate.now())) {
                        setMessage("Date of birth cannot be in the future.", false);
                        return;
                    }
                } catch (DateTimeParseException e) {
                    setMessage("Use the date format YYYY-MM-DD.", false);
                    return;
                }
            }

            boolean pwd = pwdCheckBox != null && pwdCheckBox.isSelected();
            String pwdId = pwdIdField == null ? "" : pwdIdField.getText().trim();

            if (pwd && !pwdId.matches("[A-Za-z0-9][A-Za-z0-9 -]{2,28}[A-Za-z0-9]")) {
                setMessage("Enter your PWD ID number, for example PWD-2026-0001 (letters, numbers and dashes, 4 to 30 characters).", false);
                return;
            }

            patient.setPhoneNumber(phone);
            patient.setDateOfBirth(dob);
            patient.setAddress(address);

            if (pwdCheckBox != null) {
                patient.setPwd(pwd);
                patient.setPwdIdNumber(pwdId);
            }

            showPatientType(patient);
        }

        userService.renameUser(user, name);
        user.setEmail(email);

        refreshHeader();
        setMessage("Profile updated successfully.", true);
    }

    private String roleName() {
        if (user instanceof Doctor) return "Doctor";
        if (user instanceof Nurse) return "Nurse";
        if (user instanceof Secretary) return "Secretary";
        if (user instanceof Patient) return "Patient";
        return "Administrator";
    }

    private String roleDescription() {
        if (user instanceof Doctor doctor) {
            return "Doctor  •  " + nullToEmpty(doctor.getSpecialty());
        }
        return roleName();
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty() && Character.isLetter(part.charAt(0))
                    && !part.endsWith(".")) {
                builder.append(Character.toUpperCase(part.charAt(0)));
            }
            if (builder.length() == 2) {
                break;
            }
        }

        if (builder.length() == 0 && !name.isBlank()) {
            builder.append(Character.toUpperCase(name.trim().charAt(0)));
        }

        return builder.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** Fills the patient-type block: the calculated type, the PWD box and the PWD ID. */
    private void loadPatientType(Patient patient) {
        if (pwdCheckBox == null || pwdIdField == null) {
            return;
        }

        pwdCheckBox.setSelected(patient.isPwd());
        pwdIdField.setText(patient.getPwdIdNumber());
        pwdIdField.setVisible(patient.isPwd());
        pwdIdField.setManaged(patient.isPwd());

        // The ID box only shows while the PWD box is ticked.
        pwdCheckBox.selectedProperty().addListener((observable, was, now) -> {
            pwdIdField.setVisible(now);
            pwdIdField.setManaged(now);
        });

        showPatientType(patient);
    }

    /** "Senior (age 64)", "PWD", "Senior + PWD (age 64)" or "Regular". */
    private void showPatientType(Patient patient) {
        if (patientTypeLabel == null) {
            return;
        }

        PatientType type = patient.getPatientType();
        String text = type.getLabel()
                + (type == PatientType.SENIOR || type == PatientType.SENIOR_PWD
                ? " (age " + patient.getAge() + ")" : "");

        patientTypeLabel.setText(text);
        patientTypeLabel.getStyleClass().removeAll("patient-type-priority", "patient-type-regular");
        patientTypeLabel.getStyleClass().add(
                type.isPriority() ? "patient-type-priority" : "patient-type-regular");
    }

    /** Opens the Change Password card (current password, new password, confirm). */
    @FXML
    private void handleChangePassword() {
        ChangePasswordDialog.show(saveButton.getScene());
    }

    private void setMessage(String text, boolean success) {
        messageLabel.setText(text);
        messageLabel.getStyleClass().removeAll("message-success", "message-error");
        messageLabel.getStyleClass().add(success ? "message-success" : "message-error");
    }
}
