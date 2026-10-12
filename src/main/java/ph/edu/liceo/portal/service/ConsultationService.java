package ph.edu.liceo.portal.service;

import java.util.ArrayList;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.ConsultationRequest;
import ph.edu.liceo.portal.model.Notification;

public class ConsultationService {

    private final NotificationService notifications = new NotificationService();

    public boolean createRequest(ConsultationRequest request) {

        if (request == null || isBlank(request.getPatientName())
                || isBlank(request.getDoctorName()) || isBlank(request.getReason())
                || isBlank(request.getDate())) {
            return false;
        }

        // One open request per patient, doctor and day (no accidental double-clicks).
        for (ConsultationRequest existing : DataStore.consultationRequests) {
            if (existing.getPatientName().equalsIgnoreCase(request.getPatientName())
                    && existing.getDoctorName().equalsIgnoreCase(request.getDoctorName())
                    && existing.getDate().equals(request.getDate())
                    && isOpen(existing.getStatus())) {
                return false;
            }
        }

        DataStore.consultationRequests.add(request);

        notifications.notifyByName(request.getDoctorName(),
                "New consultation request",
                request.getPatientName() + " asked for a consultation on "
                        + request.getDate() + ": " + request.getReason(),
                Notification.Type.INFO);

        return true;
    }

    private static boolean isOpen(String status) {
        return "PENDING".equals(status) || "APPROVED".equals(status)
                || "IN_PROGRESS".equals(status);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public ArrayList<ConsultationRequest> getAllRequests() {
        return DataStore.consultationRequests;
    }

    public ArrayList<ConsultationRequest> getPatientRequests(String patientName) {

        ArrayList<ConsultationRequest> result = new ArrayList<>();

        for (ConsultationRequest request : DataStore.consultationRequests) {

            if (request.getPatientName().equalsIgnoreCase(patientName)) {
                result.add(request);
            }
        }

        return result;
    }

    public ArrayList<ConsultationRequest> getDoctorRequests(String doctorName) {

        ArrayList<ConsultationRequest> result = new ArrayList<>();

        for (ConsultationRequest request : DataStore.consultationRequests) {

            if (request.getDoctorName().equalsIgnoreCase(doctorName)) {
                result.add(request);
            }
        }

        return result;
    }

    public void approveRequest(ConsultationRequest request) {

        if (request != null && "PENDING".equals(request.getStatus())) {
            request.setStatus("APPROVED");

            notifications.notifyByName(request.getPatientName(),
                    "Consultation request approved",
                    request.getDoctorName() + " approved your consultation request for "
                            + request.getDate() + ".",
                    Notification.Type.SUCCESS);
        }
    }

    public void rejectRequest(ConsultationRequest request) {

        if (request != null && "PENDING".equals(request.getStatus())) {
            request.setStatus("REJECTED");

            notifications.notifyByName(request.getPatientName(),
                    "Consultation request declined",
                    request.getDoctorName() + " could not accept your consultation request for "
                            + request.getDate() + ".",
                    Notification.Type.ALERT);
        }
    }

    public void setPending(ConsultationRequest request) {

        if (request != null) {
            request.setStatus("PENDING");
        }
    }
}