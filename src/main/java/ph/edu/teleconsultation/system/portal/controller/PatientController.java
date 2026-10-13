package ph.edu.teleconsultation.system.portal.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.ConsultationRequest;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.User;

public class PatientController extends PageController {

    @FXML private TextField searchField;
    @FXML private Label countLabel;
    @FXML private VBox patientList;

    @FXML
    private void initialize() {
        loadPatients("");
    }

    @FXML
    private void handleSearch() {
        loadPatients(searchField.getText().trim());
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        loadPatients("");
    }

    private boolean isPatientOf(Doctor doctor, String patientName) {
        for (Appointment a : DataStore.appointments) {
            if (a.getDoctorName().equalsIgnoreCase(doctor.getFullName())
                    && a.getPatientName().equalsIgnoreCase(patientName)
                    && a.isForwardedToDoctor()) {
                return true;
            }
        }

        for (ConsultationRequest r : DataStore.consultationRequests) {
            if (r.getDoctorName().equalsIgnoreCase(doctor.getFullName())
                    && r.getPatientName().equalsIgnoreCase(patientName)) {
                return true;
            }
        }

        return false;
    }

    private void loadPatients(String query) {
        patientList.getChildren().clear();

        int count = 0;

        for (User user : DataStore.users) {
            if (!(user instanceof Patient patient)) {
                continue;
            }

            String name = patient.getFullName();
            String email = patient.getEmail();

            // A doctor only sees patients who have booked or consulted with them.
            if (LoginController.getLoggedInUser() instanceof Doctor doctor
                    && !isPatientOf(doctor, name)) {
                continue;
            }

            if (!query.isBlank()
                    && !name.toLowerCase().contains(query.toLowerCase())
                    && !email.toLowerCase().contains(query.toLowerCase())) {
                continue;
            }

            VBox card = new VBox(5);
            card.getStyleClass().add("patient-card");

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("patient-card-name");

            Label details = new Label(
                    email + "  •  DOB: " + patient.getDateOfBirth()
                            + (patient.getPatientType().isPriority()
                            ? "  •  " + patient.getPatientType().getLabel() : "")
            );
            details.getStyleClass().add("patient-card-details");

            card.getChildren().addAll(nameLabel, details);
            patientList.getChildren().add(card);
            count++;
        }

        countLabel.setText(String.valueOf(count));
    }
}
