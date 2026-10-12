package ph.edu.liceo.portal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.AccountStatus;
import ph.edu.liceo.portal.model.Appointment;
import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.Notification;
import ph.edu.liceo.portal.model.Nurse;
import ph.edu.liceo.portal.model.Patient;
import ph.edu.liceo.portal.model.PatientType;
import ph.edu.liceo.portal.model.Secretary;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.model.VitalSigns;
import ph.edu.liceo.portal.service.AppointmentService;
import ph.edu.liceo.portal.service.NotificationService;

/**
 * Plain-Java checks for the Senior / PWD / Regular priority feature (no JavaFX, no JUnit).
 * Run the main method (in IntelliJ: click the green arrow next to it). It prints one line per
 * check and ends with "ALL CHECKS PASSED" or exits with an error.
 */
public class PriorityChecks {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        patientTypeRules();
        ageRules();
        appointmentDefaults();
        queueOrder();
        tableOrder();
        notificationsAndWorkflow();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");

        if (failed > 0) {
            System.exit(1);
        }

        System.out.println("ALL CHECKS PASSED");
    }

    // ------------------------------------------------------------------ rules

    private static void patientTypeRules() {
        check("regular when neither senior nor PWD", PatientType.of(false, false) == PatientType.REGULAR);
        check("senior when only senior", PatientType.of(true, false) == PatientType.SENIOR);
        check("PWD when only PWD", PatientType.of(false, true) == PatientType.PWD);
        check("senior + PWD when both", PatientType.of(true, true) == PatientType.SENIOR_PWD);
        check("regular is not priority", !PatientType.REGULAR.isPriority());
        check("senior, PWD and both are priority",
                PatientType.SENIOR.isPriority() && PatientType.PWD.isPriority()
                        && PatientType.SENIOR_PWD.isPriority());
    }

    private static void ageRules() {
        LocalDate today = LocalDate.now();

        Patient exactly60 = patient(1, "Exactly Sixty", today.minusYears(60).toString());
        Patient oneDayShort = patient(2, "Almost Sixty", today.minusYears(60).plusDays(1).toString());
        Patient young = patient(3, "Young Adult", "2004-01-15");
        Patient badDate = patient(4, "Bad Date", "not-a-date");
        Patient blank = patient(5, "Blank Date", "");
        Patient future = patient(6, "Future Born", today.plusDays(3).toString());

        check("age 60 today is a senior", exactly60.isSenior() && exactly60.getAge() == 60);
        check("one day before the 60th birthday is not a senior", !oneDayShort.isSenior() && oneDayShort.getAge() == 59);
        check("young adult is regular", young.getPatientType() == PatientType.REGULAR);
        check("unreadable date of birth does not crash and is regular",
                badDate.getAge() == -1 && badDate.getPatientType() == PatientType.REGULAR);
        check("blank date of birth is regular", blank.getPatientType() == PatientType.REGULAR);
        check("future date of birth is regular", future.getPatientType() == PatientType.REGULAR);

        young.setPwd(true);
        young.setPwdIdNumber("  PWD-2026-0007 ");
        check("declared PWD becomes PWD type", young.getPatientType() == PatientType.PWD);
        check("PWD id number is trimmed", "PWD-2026-0007".equals(young.getPwdIdNumber()));

        exactly60.setPwd(true);
        check("senior who is also PWD becomes SENIOR_PWD", exactly60.getPatientType() == PatientType.SENIOR_PWD);

        young.setPwd(false);
        check("clearing PWD clears the id number and returns to regular",
                young.getPatientType() == PatientType.REGULAR && young.getPwdIdNumber().isEmpty());
    }

    private static void appointmentDefaults() {
        Appointment a = appointment(1, "Someone", PatientType.REGULAR);
        check("new appointment is regular by default", a.getPatientType() == PatientType.REGULAR && !a.isPriority());
        a.setPatientType(null);
        check("null type falls back to regular", a.getPatientType() == PatientType.REGULAR);
        a.setPatientType(PatientType.SENIOR);
        check("senior appointment is priority", a.isPriority());
    }

    // ------------------------------------------------------------------ queues

    private static void queueOrder() {
        reset();
        AppointmentService service = new AppointmentService();

        add(appointment(1, "Regular One", PatientType.REGULAR), AppointmentService.PENDING);
        add(appointment(2, "Senior Two", PatientType.SENIOR), AppointmentService.PENDING);
        add(appointment(3, "Regular Three", PatientType.REGULAR), AppointmentService.PENDING);
        add(appointment(4, "Pwd Four", PatientType.PWD), AppointmentService.PENDING);
        add(appointment(5, "Both Five", PatientType.SENIOR_PWD), AppointmentService.PENDING);
        add(appointment(6, "Forwarded Six", PatientType.SENIOR), AppointmentService.FOR_DOCTOR_REVIEW);

        Queue<Appointment> nurse = service.getNurseReviewQueue();
        check("nurse queue only holds PENDING requests", nurse.size() == 5);
        check("nurse queue: priority first, then first come first served",
                ids(drain(nurse)).equals(List.of(2, 4, 5, 1, 3)));

        reset();
        add(appointment(1, "Regular One", PatientType.REGULAR), AppointmentService.FOR_DOCTOR_REVIEW);
        add(appointment(2, "Regular Two", PatientType.REGULAR), AppointmentService.FOR_DOCTOR_REVIEW);
        add(appointment(3, "Senior Three", PatientType.SENIOR), AppointmentService.FOR_DOCTOR_REVIEW);
        add(appointment(4, "Still With Nurse", PatientType.SENIOR), AppointmentService.PENDING);

        Queue<Appointment> doctor = service.getPendingQueue("Dr. Test");
        check("doctor queue only holds nurse-forwarded requests", doctor.size() == 3);
        check("doctor queue head is the senior", doctor.peek().getId() == 3);
        check("doctor queue: priority first, then arrival", ids(drain(doctor)).equals(List.of(3, 1, 2)));

        reset();
        add(appointment(1, "Regular One", PatientType.REGULAR), AppointmentService.PENDING);
        add(appointment(2, "Regular Two", PatientType.REGULAR), AppointmentService.PENDING);
        check("with no priority patients the order is still first come first served",
                ids(drain(service.getNurseReviewQueue())).equals(List.of(1, 2)));
    }

    private static void tableOrder() {
        reset();
        // Two dates; the senior asked later and has a later slot than the regular one.
        Appointment regularEarly = appointment(1, "Regular Early", PatientType.REGULAR);
        Appointment seniorLate = appointment(2, "Senior Late", PatientType.SENIOR);
        Appointment doneOne = appointment(3, "Already Forwarded", PatientType.REGULAR);
        regularEarly.setStatus(AppointmentService.PENDING);
        seniorLate.setStatus(AppointmentService.PENDING);
        doneOne.setStatus(AppointmentService.FOR_DOCTOR_REVIEW);

        List<Appointment> nurseTable = new ArrayList<>(List.of(doneOne, regularEarly, seniorLate));
        nurseTable.sort(AppointmentService.NURSE_FIRST);
        check("nurse table: senior pending request above the regular one, other statuses after",
                ids(nurseTable).equals(List.of(2, 1, 3)));

        Appointment awaiting = appointment(4, "Awaiting Doctor", PatientType.REGULAR);
        Appointment awaitingSenior = appointment(5, "Awaiting Senior", PatientType.PWD);
        Appointment pending = appointment(6, "Pending", PatientType.SENIOR);
        awaiting.setStatus(AppointmentService.FOR_DOCTOR_REVIEW);
        awaitingSenior.setStatus(AppointmentService.FOR_DOCTOR_REVIEW);
        pending.setStatus(AppointmentService.PENDING);
        List<Appointment> doctorTable = new ArrayList<>(List.of(pending, awaiting, awaitingSenior));
        doctorTable.sort(AppointmentService.DOCTOR_FIRST);
        check("doctor table: waiting requests first, PWD before regular among them",
                ids(doctorTable).equals(List.of(5, 4, 6)));

        Appointment appr1 = appointment(7, "Approved Regular", PatientType.REGULAR);
        Appointment appr2 = appointment(8, "Approved Senior", PatientType.SENIOR);
        appr1.setStatus(AppointmentService.DOCTOR_APPROVED);
        appr2.setStatus(AppointmentService.DOCTOR_APPROVED);
        List<Appointment> secTable = new ArrayList<>(List.of(appr1, appr2));
        secTable.sort(AppointmentService.SECRETARY_FIRST);
        check("secretary table: approved senior above approved regular", ids(secTable).equals(List.of(8, 7)));
    }

    // ------------------------------------------------- notifications + workflow

    private static void notificationsAndWorkflow() {
        reset();
        Nurse nurse = new Nurse(DataStore.getNextUserId(), "Nurse Test", "nurse@t.com", "pw", "RN-1", "Clinic");
        nurse.setStatus(AccountStatus.ACTIVE);
        Doctor doctor = new Doctor(DataStore.getNextUserId(), "Dr. Test", "doc@t.com", "pw", "MD-1", "General Medicine");
        doctor.setStatus(AccountStatus.ACTIVE);
        Secretary secretary = new Secretary(DataStore.getNextUserId(), "Sec Test", "sec@t.com", "pw", "Admin");
        secretary.setStatus(AccountStatus.ACTIVE);
        Patient senior = patient(DataStore.getNextUserId(), "Lola Senior", LocalDate.now().minusYears(70).toString());
        Patient regular = patient(DataStore.getNextUserId(), "Regular Person", "2004-01-15");
        DataStore.users.add(nurse);
        DataStore.users.add(doctor);
        DataStore.users.add(secretary);
        DataStore.users.add(senior);
        DataStore.users.add(regular);

        AppointmentService service = new AppointmentService();
        NotificationService notes = new NotificationService();
        LocalDate day = LocalDate.now().plusDays(3);

        Appointment regularRequest = new Appointment(DataStore.getNextAppointmentId(), regular.getFullName(),
                doctor.getFullName(), "General Medicine", day.toString(), "09:00 AM", "General Consultation", "x");
        regularRequest.setPatientType(regular.getPatientType());
        check("regular request is created", service.createAppointment(regularRequest));

        Appointment seniorRequest = new Appointment(DataStore.getNextAppointmentId(), senior.getFullName(),
                doctor.getFullName(), "General Medicine", day.toString(), "10:00 AM", "General Consultation", "x");
        seniorRequest.setPatientType(senior.getPatientType());
        check("senior request is created", service.createAppointment(seniorRequest));

        List<Notification> inbox = notes.getNotifications(nurse);   // newest first
        check("nurse got one notice per request", inbox.size() == 2);
        check("senior request produced a priority notice (warning)",
                inbox.get(0).getTitle().equals("Priority request to review")
                        && inbox.get(0).getType() == Notification.Type.WARNING
                        && inbox.get(0).getMessage().contains("Lola Senior (Senior)"));
        check("regular request produced the normal notice (info)",
                inbox.get(1).getTitle().equals("New request to review")
                        && inbox.get(1).getType() == Notification.Type.INFO);

        check("nurse sees the senior first", service.getNurseReviewQueue().peek() == seniorRequest);

        // Full workflow still works for a priority patient.
        check("nurse cannot forward without vitals", !service.forwardByNurse(seniorRequest));
        check("nurse records vitals", service.recordVitals(seniorRequest, new VitalSigns(36.8, 72, 120, 80, 16)));
        check("nurse forwards the senior request", service.forwardByNurse(seniorRequest));
        check("doctor now has the senior at the head of the queue",
                service.getPendingQueue(doctor.getFullName()).peek() == seniorRequest);

        List<Notification> doctorInbox = notes.getNotifications(doctor);
        check("doctor got a priority notice",
                doctorInbox.get(0).getTitle().equals("Priority request ready for your review"));

        check("doctor approves", service.approveByDoctor(seniorRequest));
        check("secretary confirms", service.confirmBySecretary(seniorRequest));
        check("status is CONFIRMED", AppointmentService.CONFIRMED.equals(seniorRequest.getStatus()));
        senior.setDateOfBirth("2004-01-15");   // the profile changes later...
        check("type stays saved on the appointment even if the profile changes later",
                seniorRequest.getPatientType() == PatientType.SENIOR
                        && senior.getPatientType() == PatientType.REGULAR);
        check("doctor does not see the regular request yet (not forwarded)",
                service.getDoctorAppointments(doctor.getFullName()).size() == 1);
    }

    // ----------------------------------------------------------------- helpers

    private static Patient patient(int id, String name, String dob) {
        return new Patient(id, name, name.toLowerCase().replace(' ', '.') + "@gmail.com", "pw", dob, "0917", "CDO");
    }

    private static Appointment appointment(int id, String patientName, PatientType type) {
        Appointment a = new Appointment(id, patientName, "Dr. Test", "General Medicine",
                LocalDate.now().plusDays(5).toString(), "09:00 AM", "General Consultation", "note");
        a.setPatientType(type);
        return a;
    }

    private static void add(Appointment a, String status) {
        a.setStatus(status);
        DataStore.appointments.add(a);
    }

    private static List<Appointment> drain(Queue<Appointment> queue) {
        List<Appointment> out = new ArrayList<>();
        while (!queue.isEmpty()) {
            out.add(queue.poll());
        }
        return out;
    }

    private static List<Integer> ids(List<Appointment> list) {
        List<Integer> out = new ArrayList<>();
        for (Appointment a : list) {
            out.add(a.getId());
        }
        return out;
    }

    private static void reset() {
        DataStore.users.clear();
        DataStore.appointments.clear();
        DataStore.notifications.clear();
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }
}
