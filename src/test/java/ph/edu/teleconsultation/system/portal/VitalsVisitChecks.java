package ph.edu.teleconsultation.system.portal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.AccountStatus;
import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.Doctor;
import ph.edu.teleconsultation.system.portal.model.Notification;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.Patient;
import ph.edu.teleconsultation.system.portal.model.VitalSigns;
import ph.edu.teleconsultation.system.portal.service.AppointmentService;
import ph.edu.teleconsultation.system.portal.service.NotificationService;

/**
 * Plain-Java checks for the "vitals check-in" feature: the nurse asks a patient without a home
 * device to come to the clinic for vitals before the appointment (no JavaFX, no JUnit).
 * Run the main method; it ends with "ALL CHECKS PASSED" or exits with an error.
 */
public class VitalsVisitChecks {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        rules();
        timesOffered();
        workflow();
        displayText();

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");

        if (failed > 0) {
            System.exit(1);
        }

        System.out.println("ALL CHECKS PASSED");
    }

    private static void rules() {
        reset();
        AppointmentService service = new AppointmentService();
        LocalDate in3 = LocalDate.now().plusDays(3);
        Appointment a = request(1, "Pat Ient", in3, "10:00 AM");

        check("a new request without vitals can get a clinic check-in", service.vitalsVisitProblem(a) == null);
        check("a visit the day before the appointment is accepted",
                service.vitalsVisitProblem(a, in3.minusDays(1), "09:00 AM") == null);
        check("a visit exactly 60 minutes before is accepted",
                service.vitalsVisitProblem(a, in3, "09:00 AM") == null);
        check("a visit 30 minutes before is refused (too late to forward)",
                service.vitalsVisitProblem(a, in3, "09:30 AM") != null);
        check("a visit after the appointment is refused",
                service.vitalsVisitProblem(a, in3, "11:00 AM") != null);
        check("a visit in the past is refused",
                service.vitalsVisitProblem(a, LocalDate.now().minusDays(1), "09:00 AM") != null);
        check("no date is refused", service.vitalsVisitProblem(a, null, "09:00 AM") != null);
        check("unreadable time is refused", service.vitalsVisitProblem(a, in3, "soon") != null);
        check("scheduling an invalid time returns false and changes nothing",
                !service.scheduleVitalsCheck(a, in3, "11:00 AM", "") && !a.hasVitalsCheck());

        Appointment forwarded = request(2, "Pat Ient", in3, "10:00 AM");
        forwarded.setStatus(AppointmentService.FOR_DOCTOR_REVIEW);
        check("only requests still in nurse review can get a visit", service.vitalsVisitProblem(forwarded) != null);

        Appointment checked = request(3, "Pat Ient", in3, "10:00 AM");
        service.recordVitals(checked, new VitalSigns(36.8, 72, 120, 80, 16));
        check("no visit needed once a nurse recorded the vitals", service.vitalsVisitProblem(checked) != null);

        check("null appointment is handled", service.vitalsVisitProblem(null) != null);
    }

    private static void timesOffered() {
        reset();
        AppointmentService service = new AppointmentService();
        LocalDate in3 = LocalDate.now().plusDays(3);
        Appointment a = request(1, "Pat Ient", in3, "10:00 AM");

        List<String> day = service.vitalsVisitTimes(a, in3);
        check("on the appointment day only times up to 60 minutes before are offered",
                day.equals(List.of("08:00 AM", "08:30 AM", "09:00 AM")));

        List<String> before = service.vitalsVisitTimes(a, in3.minusDays(1));
        check("an earlier day offers the full clinic day (8:00 AM to 4:30 PM)",
                before.size() == 18 && before.get(0).equals("08:00 AM") && before.get(17).equals("04:30 PM"));

        check("a past day offers nothing", service.vitalsVisitTimes(a, LocalDate.now().minusDays(1)).isEmpty());
        check("first available day starts from today", LocalDate.now().equals(service.firstVitalsVisitDate(a))
                || LocalDate.now().plusDays(1).equals(service.firstVitalsVisitDate(a)));

        // Booked for very soon: nothing fits before it, so there is no day to offer.
        LocalDateTime soon = LocalDateTime.now().plusMinutes(20);
        Appointment tooSoon = request(2, "Pat Ient", soon.toLocalDate(), soon.toLocalTime().format(CLOCK));
        check("an appointment too close has no check-in day", service.firstVitalsVisitDate(tooSoon) == null);
    }

    private static void workflow() {
        reset();
        Nurse nurse = new Nurse(DataStore.getNextUserId(), "Nurse Test", "nurse@t.com", "pw", "RN-1", "Clinic");
        nurse.setStatus(AccountStatus.ACTIVE);
        Doctor doctor = new Doctor(DataStore.getNextUserId(), "Dr. Test", "doc@t.com", "pw", "MD-1", "General Medicine");
        doctor.setStatus(AccountStatus.ACTIVE);
        Patient patient = new Patient(DataStore.getNextUserId(), "Pat Ient", "pat@gmail.com", "pw", "2004-01-15", "0917", "CDO");
        DataStore.users.add(nurse);
        DataStore.users.add(doctor);
        DataStore.users.add(patient);

        AppointmentService service = new AppointmentService();
        NotificationService notes = new NotificationService();
        LocalDate in3 = LocalDate.now().plusDays(3);

        Appointment a = new Appointment(DataStore.getNextAppointmentId(), patient.getFullName(),
                doctor.getFullName(), "General Medicine", in3.toString(), "10:00 AM", "General Consultation", "x");
        check("patient requests without vitals", service.createAppointment(a) && a.getVitals() == null);
        check("nurse cannot forward it yet", !service.forwardByNurse(a));

        LocalDate day = in3.minusDays(1);
        check("nurse sets a clinic check-in", service.scheduleVitalsCheck(a, day, "09:00 AM", "Clinic nurse station"));
        check("check-in is stored on the request",
                a.hasVitalsCheck() && a.getVitalsCheckDate().equals(day.toString())
                        && a.getVitalsCheckTime().equals("09:00 AM"));
        check("request is still waiting for nurse review", AppointmentService.PENDING.equals(a.getStatus()));

        List<Notification> inbox = notes.getNotifications(patient);
        check("patient was told when and where to come",
                inbox.size() == 1 && inbox.get(0).getTitle().equals("Please come in for a vitals check")
                        && inbox.get(0).getMessage().contains("09:00 AM")
                        && inbox.get(0).getMessage().contains("Clinic nurse station")
                        && inbox.get(0).getType() == Notification.Type.WARNING);

        check("patient table text says scheduled", AppointmentService.vitalsCheckText(a).endsWith("scheduled"));

        check("nurse can move the check-in",
                service.scheduleVitalsCheck(a, day, "10:30 AM", ""));
        check("moving it sends a 'moved' notice",
                notes.getNotifications(patient).get(0).getTitle().equals("Vitals check moved")
                        && notes.getNotifications(patient).get(0).getMessage().contains("the clinic nurse station"));

        check("patient arrives, nurse records vitals",
                service.recordVitals(a, new VitalSigns(36.8, 72, 120, 80, 16)));
        check("vitals are marked nurse-checked", a.isVitalsChecked() && a.getVitals() != null);
        check("patient was thanked and told what happens next",
                notes.getNotifications(patient).get(0).getTitle().equals("Vitals recorded"));
        check("table text now says done", AppointmentService.vitalsCheckText(a).endsWith("done"));
        check("nurse can now forward to the doctor", service.forwardByNurse(a));
        check("no new visit can be set after that", !service.scheduleVitalsCheck(a, day, "09:00 AM", ""));

        // A request whose patient entered their own vitals at home never needs the visit.
        Appointment b = new Appointment(DataStore.getNextAppointmentId(), patient.getFullName(),
                doctor.getFullName(), "General Medicine", in3.toString(), "11:00 AM", "General Consultation", "x");
        b.setVitals(new VitalSigns(36.5, 70, 118, 76, 15));
        check("self-reported vitals are not mistaken for a done visit", !b.hasVitalsCheck());
        check("the nurse may still ask a self-reporting patient to come in to verify",
                service.vitalsVisitProblem(b) == null);
    }

    private static void displayText() {
        reset();
        LocalDate in3 = LocalDate.now().plusDays(3);
        Appointment a = request(1, "Pat Ient", in3, "10:00 AM");
        check("no visit means an empty column", AppointmentService.vitalsCheckText(a).isEmpty());
        check("null is safe", AppointmentService.vitalsCheckText(null).isEmpty());

        a.setVitalsCheck(LocalDate.now().minusDays(1).toString(), "09:00 AM", "");
        check("a visit that passed without vitals says 'not done yet'",
                AppointmentService.vitalsCheckText(a).endsWith("not done yet"));

        a.setStatus(AppointmentService.CANCELLED);
        check("a cancelled request shows nothing", AppointmentService.vitalsCheckText(a).isEmpty());

        a.setVitalsCheck("garbage", "09:00 AM", "");
        a.setStatus(AppointmentService.PENDING);
        check("a damaged date does not crash the table", AppointmentService.vitalsCheckText(a).isEmpty());
    }

    // ----------------------------------------------------------------- helpers

    private static Appointment request(int id, String patient, LocalDate date, String time) {
        Appointment a = new Appointment(id, patient, "Dr. Test", "General Medicine",
                date.toString(), time, "General Consultation", "note");
        return a;
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
