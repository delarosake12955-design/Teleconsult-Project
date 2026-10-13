package ph.edu.teleconsultation.system.portal.service;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Comparator;
import java.util.ArrayDeque;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.MedicalRecord;
import ph.edu.teleconsultation.system.portal.model.Notification;
import ph.edu.teleconsultation.system.portal.model.Nurse;

public class MedicalRecordService {

    public boolean addRecord(MedicalRecord record) {

        if (record == null) {
            return false;
        }

        DataStore.medicalRecords.add(record);

        NotificationService notifications = new NotificationService();

        notifications.notifyByName(record.getPatientName(),
                "New medical record",
                record.getDoctorName() + " added a record: " + record.getDiagnosis() + ".",
                Notification.Type.INFO);

        if (record.getVitalSigns() == null) {
            notifications.notifyRole(Nurse.class,
                    "Vital signs needed",
                    record.getPatientName() + " is waiting for vital signs.",
                    Notification.Type.WARNING);
        }

        return true;
    }

    /** Records still waiting for a nurse to take vitals, oldest first (FIFO). */
    public Queue<MedicalRecord> getVitalsQueue() {

        ArrayList<MedicalRecord> waiting = new ArrayList<>();

        for (MedicalRecord record : DataStore.medicalRecords) {
            if (record.getVitalSigns() == null) {
                waiting.add(record);
            }
        }

        waiting.sort(Comparator.comparingInt(MedicalRecord::getId));

        return new ArrayDeque<>(waiting);
    }

    public ArrayList<MedicalRecord> getAllRecords() {
        return DataStore.medicalRecords;
    }

    public ArrayList<MedicalRecord> getPatientRecords(String patientName) {

        ArrayList<MedicalRecord> result = new ArrayList<>();

        for (MedicalRecord record : DataStore.medicalRecords) {

            if (record.getPatientName().equalsIgnoreCase(patientName)) {
                result.add(record);
            }
        }

        return result;
    }

    public ArrayList<MedicalRecord> getDoctorRecords(String doctorName) {

        ArrayList<MedicalRecord> result = new ArrayList<>();

        for (MedicalRecord record : DataStore.medicalRecords) {

            if (record.getDoctorName().equalsIgnoreCase(doctorName)) {
                result.add(record);
            }
        }

        return result;
    }

    public MedicalRecord findById(int id) {

        for (MedicalRecord record : DataStore.medicalRecords) {

            if (record.getId() == id) {
                return record;
            }
        }

        return null;
    }

    public void deleteRecord(MedicalRecord record) {

        if (record != null) {
            DataStore.medicalRecords.remove(record);
        }
    }
}