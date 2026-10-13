package ph.edu.teleconsultation.system.portal.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Comparator;
import java.util.Objects;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Appointment;
import ph.edu.teleconsultation.system.portal.model.MedicalRecord;
import ph.edu.teleconsultation.system.portal.model.Notification;
import ph.edu.teleconsultation.system.portal.model.Nurse;
import ph.edu.teleconsultation.system.portal.model.Schedule;
import ph.edu.teleconsultation.system.portal.model.Secretary;
import ph.edu.teleconsultation.system.portal.model.VitalSigns;

/** Business rules for appointment requests and the doctor/secretary workflow. */
public class AppointmentService {

    private final NotificationService notifications = new NotificationService();

    public static final String PENDING = "PENDING";
    public static final String FOR_DOCTOR_REVIEW = "FOR_DOCTOR_REVIEW";
    public static final String DOCTOR_APPROVED = "DOCTOR_APPROVED";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String COMPLETED = "COMPLETED";

    /** How many upcoming, active appointments one patient may hold at the same time. */
    public static final int MAX_ACTIVE_PER_PATIENT = 3;

    /** Requests and bookings that still hold a slot in the future (not rejected, cancelled or past). */
    public ArrayList<Appointment> getActiveAppointments(String patientName) {
        ArrayList<Appointment> result = new ArrayList<>();
        if (patientName == null) return result;
        for (Appointment a : DataStore.appointments) {
            if (a.getPatientName() == null || !a.getPatientName().equalsIgnoreCase(patientName)) continue;
            String st = a.getStatus();
            boolean open = PENDING.equalsIgnoreCase(st) || FOR_DOCTOR_REVIEW.equalsIgnoreCase(st)
                    || DOCTOR_APPROVED.equalsIgnoreCase(st) || CONFIRMED.equalsIgnoreCase(st);
            if (!open) continue;
            try {
                if (ScheduleService.hasPassed(LocalDate.parse(a.getDate()), a.getTime())) continue;
            } catch (RuntimeException e) {
                continue;
            }
            result.add(a);
        }
        return result;
    }

    public int activeCount(String patientName) {
        return getActiveAppointments(patientName).size();
    }

    /** Null when the patient may hold another appointment, otherwise why not. */
    public String activeLimitProblem(String patientName) {
        if (activeCount(patientName) >= MAX_ACTIVE_PER_PATIENT) {
            return "Limit reached: " + patientName + " can have up to " + MAX_ACTIVE_PER_PATIENT
                    + " active appointments at a time. Wait for one to be completed or cancel one first.";
        }
        return null;
    }

    public boolean createAppointment(Appointment appointment) {
        if (appointment == null || isBlank(appointment.getPatientName())
                || isBlank(appointment.getDoctorName()) || isBlank(appointment.getDate())
                || isBlank(appointment.getTime())) {
            return false;
        }

        if (activeLimitProblem(appointment.getPatientName()) != null) {
            return false;
        }

        // Appointments can only be requested for a real date/time that has not passed.
        try {
            if (ScheduleService.hasPassed(
                    LocalDate.parse(appointment.getDate()), appointment.getTime())) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }

        // Guard against duplicate slots even if a caller bypasses the calendar UI.
        for (Appointment existing : DataStore.appointments) {
            if (sameSlot(existing, appointment) && holdsSlot(existing)) {
                return false;
            }
        }

        DataStore.appointments.add(appointment);

        // SRS workflow: the nurse reviews every new request before the doctor sees it.
        // Seniors and PWDs are priority patients: say so, so the nurse reviews them first.
        boolean priority = appointment.isPriority();
        String who = priority
                ? appointment.getPatientName() + " (" + appointment.getPatientType().getLabel() + ")"
                : appointment.getPatientName();

        notifications.notifyRole(Nurse.class,
                priority ? "Priority request to review" : "New request to review",
                who + " requested " + appointment.getDoctorName()
                        + " on " + appointment.getDate() + " at " + appointment.getTime()
                        + ". Check the details and vitals, then forward or return it."
                        + (priority ? " Review priority patients first." : ""),
                priority ? Notification.Type.WARNING : Notification.Type.INFO);

        return true;
    }

    public ArrayList<Appointment> getAllAppointments() {
        return new ArrayList<>(DataStore.appointments);
    }

    public ArrayList<Appointment> getPatientAppointments(String patientName) {
        ArrayList<Appointment> result = new ArrayList<>();
        if (patientName == null) return result;
        for (Appointment appointment : DataStore.appointments) {
            if (appointment.getPatientName() != null
                    && appointment.getPatientName().equalsIgnoreCase(patientName)
                    && !appointment.isHiddenFromPatient()) {
                result.add(appointment);
            }
        }
        return result;
    }

    /**
     * The doctor's waiting list as a priority queue: only requests a nurse has forwarded
     * (FOR_DOCTOR_REVIEW). Senior and PWD requests are served first; within each group the
     * request that arrived first (lowest id) is at the head.
     */
    public Queue<Appointment> getPendingQueue(String doctorName) {

        PriorityQueue<Appointment> waiting = new PriorityQueue<>(PRIORITY_THEN_ARRIVAL);

        for (Appointment a : DataStore.appointments) {
            if (a.getDoctorName().equalsIgnoreCase(doctorName)
                    && FOR_DOCTOR_REVIEW.equalsIgnoreCase(a.getStatus())) {
                waiting.add(a);
            }
        }

        return waiting;
    }

    /**
     * The nurses' shared to-do list: new requests nobody has reviewed yet, as a priority
     * queue. Senior and PWD requests come first, then the oldest request (lowest id).
     */
    public Queue<Appointment> getNurseReviewQueue() {
        PriorityQueue<Appointment> waiting = new PriorityQueue<>(PRIORITY_THEN_ARRIVAL);

        for (Appointment a : DataStore.appointments) {
            if (PENDING.equalsIgnoreCase(a.getStatus())) {
                waiting.add(a);
            }
        }

        return waiting;
    }

    /**
     * Nurse review, step 1 of the SRS workflow. A request is complete only when
     * it carries vitals (reported by the patient or entered by the nurse); then
     * it is forwarded and the doctor and patient are told.
     */
    public boolean forwardByNurse(Appointment appointment) {
        if (appointment == null || !PENDING.equalsIgnoreCase(appointment.getStatus())
                || appointment.getVitals() == null) {
            return false;
        }

        appointment.setStatus(FOR_DOCTOR_REVIEW);

        boolean priority = appointment.isPriority();

        notifications.notifyByName(appointment.getDoctorName(),
                priority ? "Priority request ready for your review" : "Request ready for your review",
                appointment.getPatientName()
                        + (priority ? " (" + appointment.getPatientType().getLabel() + ")" : "")
                        + " on " + appointment.getDate() + " at "
                        + appointment.getTime() + ". Vitals: " + appointment.getVitals().summary(),
                priority ? Notification.Type.WARNING : Notification.Type.INFO);

        notifications.notifyByName(appointment.getPatientName(),
                "Request forwarded to the doctor",
                "A nurse checked your request. " + appointment.getDoctorName()
                        + " will review it next.",
                Notification.Type.INFO);

        return true;
    }

    /** Nurse sends an incomplete request back; the slot is freed and the patient is told why. */
    public boolean returnByNurse(Appointment appointment, String reason) {
        if (appointment == null || !PENDING.equalsIgnoreCase(appointment.getStatus())) {
            return false;
        }

        appointment.setStatus(REJECTED);
        restoreSlot(appointment);

        String why = isBlank(reason) ? "some details were incomplete" : reason.trim();
        appointment.setReturnReason(why);

        notifications.notifyByName(appointment.getPatientName(),
                "Request returned by the nurse",
                "Your request with " + appointment.getDoctorName() + " on "
                        + appointment.getDate() + " was returned: " + why
                        + ". The nurse may reconsider it, or you can submit a new request with complete details.",
                Notification.Type.ALERT);

        return true;
    }

    /** The doctor's own list: only requests a nurse has forwarded (see isForwardedToDoctor). */
    public ArrayList<Appointment> getDoctorAppointments(String doctorName) {
        ArrayList<Appointment> result = new ArrayList<>();
        if (doctorName == null) return result;
        for (Appointment appointment : DataStore.appointments) {
            // Requests the nurse is still reviewing (or returned) are not the doctor's yet.
            if (appointment.getDoctorName() != null
                    && appointment.getDoctorName().equalsIgnoreCase(doctorName)
                    && appointment.isForwardedToDoctor()) {
                result.add(appointment);
            }
        }
        return result;
    }

    /** Doctor review is the first approval step; secretary confirmation is final. */
    public boolean approveByDoctor(Appointment appointment) {
        if (appointment == null || !FOR_DOCTOR_REVIEW.equalsIgnoreCase(appointment.getStatus())) return false;
        appointment.setStatus(DOCTOR_APPROVED);

        notifications.notifyByName(appointment.getPatientName(),
                "Appointment approved by doctor",
                appointment.getDoctorName() + " approved your request for "
                        + appointment.getDate() + " at " + appointment.getTime()
                        + ". The secretary will confirm it.",
                Notification.Type.SUCCESS);

        notifications.notifyRole(Secretary.class,
                "Appointment needs confirmation",
                appointment.getPatientName() + " with " + appointment.getDoctorName()
                        + " on " + appointment.getDate() + " at " + appointment.getTime()
                        + " is waiting to be confirmed.",
                Notification.Type.WARNING);

        return true;
    }

    public boolean confirmBySecretary(Appointment appointment) {
        if (appointment == null || !DOCTOR_APPROVED.equalsIgnoreCase(appointment.getStatus())) return false;
        appointment.setStatus(CONFIRMED);

        notifications.notifyByName(appointment.getPatientName(),
                "Appointment confirmed",
                "Your appointment with " + appointment.getDoctorName() + " on "
                        + appointment.getDate() + " at " + appointment.getTime()
                        + " is confirmed.",
                Notification.Type.SUCCESS);

        notifications.notifyByName(appointment.getDoctorName(),
                "Appointment confirmed",
                appointment.getPatientName() + " on " + appointment.getDate()
                        + " at " + appointment.getTime() + " is confirmed.",
                Notification.Type.INFO);

        return true;
    }

    public boolean rejectAppointment(Appointment appointment) {
        if (appointment == null || REJECTED.equalsIgnoreCase(appointment.getStatus())
                || CONFIRMED.equalsIgnoreCase(appointment.getStatus())) return false;
        boolean wasDoctorApproved = DOCTOR_APPROVED.equalsIgnoreCase(appointment.getStatus());
        appointment.setStatus(REJECTED);

        notifications.notifyByName(appointment.getPatientName(),
                "Appointment request declined",
                "Your request with " + appointment.getDoctorName() + " on "
                        + appointment.getDate() + " at " + appointment.getTime()
                        + " was declined. You can choose another time.",
                Notification.Type.ALERT);

        if (wasDoctorApproved) {
            notifications.notifyByName(appointment.getDoctorName(),
                    "Approved request was declined",
                    appointment.getPatientName() + " on " + appointment.getDate()
                            + " at " + appointment.getTime()
                            + " was declined by the secretary.",
                    Notification.Type.WARNING);
        }

        // The slot was held when the patient submitted the request. Return it if rejected.
        try {
            LocalDate date = LocalDate.parse(appointment.getDate());
            new ScheduleService().restoreTime(
                    appointment.getDoctorName(), date, appointment.getTime());
        } catch (RuntimeException ignored) {
            // Keep the status transition safe even if an old record has an invalid date.
        }
        return true;
    }

    /**
     * The secretary only handles requests the doctor has already approved;
     * a request still waiting for the doctor cannot be rejected from here.
     */
    public boolean rejectBySecretary(Appointment appointment) {
        if (appointment == null || !DOCTOR_APPROVED.equalsIgnoreCase(appointment.getStatus())) {
            return false;
        }
        return rejectAppointment(appointment);
    }

    public void approveAppointment(Appointment appointment) {
        approveByDoctor(appointment);
    }

    public void setPending(Appointment appointment) {
        if (appointment != null) appointment.setStatus(PENDING);
    }

    public void deleteAppointment(Appointment appointment) {
        if (appointment == null) return;
        // A removed request that still held a slot must give that slot back.
        if (holdsSlot(appointment)) {
            try {
                new ScheduleService().restoreTime(appointment.getDoctorName(),
                        LocalDate.parse(appointment.getDate()), appointment.getTime());
            } catch (RuntimeException ignored) {
                // Old record with an unreadable date: just remove it.
            }
        }
        DataStore.appointments.remove(appointment);
    }

    /**
     * Patient self-service: a patient may cancel their own request or booking
     * while it is still PENDING, DOCTOR_APPROVED or CONFIRMED and the date has
     * not passed. The slot goes back to the doctor's calendar and the people
     * involved are told.
     */
    public boolean cancelByPatient(Appointment appointment, String patientName) {
        if (appointment == null || patientName == null
                || appointment.getPatientName() == null
                || !appointment.getPatientName().equalsIgnoreCase(patientName)) {
            return false;
        }

        String status = appointment.getStatus();
        if (!(PENDING.equalsIgnoreCase(status) || FOR_DOCTOR_REVIEW.equalsIgnoreCase(status)
                || DOCTOR_APPROVED.equalsIgnoreCase(status) || CONFIRMED.equalsIgnoreCase(status))) {
            return false;
        }

        try {
            if (ScheduleService.hasPassed(LocalDate.parse(appointment.getDate()),
                    appointment.getTime())) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }

        boolean needsSecretary = DOCTOR_APPROVED.equalsIgnoreCase(status)
                || CONFIRMED.equalsIgnoreCase(status);
        appointment.setStatus(CANCELLED);
        appointment.setCancelledByPatient(true);

        try {
            new ScheduleService().restoreTime(appointment.getDoctorName(),
                    LocalDate.parse(appointment.getDate()), appointment.getTime());
        } catch (RuntimeException ignored) {
            // Keep the status change even if an old record has an unreadable date.
        }

        String detail = appointment.getPatientName() + " cancelled the appointment on "
                + appointment.getDate() + " at " + appointment.getTime() + ".";

        notifications.notifyByName(appointment.getDoctorName(),
                "Appointment cancelled", detail, Notification.Type.WARNING);

        if (needsSecretary) {
            notifications.notifyRole(Secretary.class,
                    "Appointment cancelled", detail, Notification.Type.INFO);
        }

        return true;
    }


    // ------------------------------------------------------ reconsider / remove

    /**
     * Why a returned or cancelled request cannot be put back for nurse review, or null when it can.
     * The date must not have passed and nobody else may hold the doctor's slot now.
     */
    private String reviveProblem(Appointment appointment) {
        LocalDate date;
        try {
            date = LocalDate.parse(appointment.getDate());
        } catch (RuntimeException e) {
            return "This request has an unreadable date.";
        }

        if (ScheduleService.hasPassed(date, appointment.getTime())) {
            return "That appointment time has already passed.";
        }

        String limit = activeLimitProblem(appointment.getPatientName());
        if (limit != null) {
            return limit;
        }

        for (Appointment other : DataStore.appointments) {
            if (other != appointment && sameSlot(other, appointment) && holdsSlot(other)) {
                return "Someone else has already taken that time with " + appointment.getDoctorName() + ".";
            }
        }

        ScheduleService schedule = new ScheduleService();
        if (!schedule.getBookableTimes(appointment.getDoctorName(), date).contains(appointment.getTime())) {
            return "That time is no longer open on the doctor's calendar.";
        }

        return null;
    }

    /** Puts the request back in the nurse's review list and holds the doctor's slot again. */
    private void revive(Appointment appointment) {
        new ScheduleService().bookTime(appointment.getDoctorName(),
                LocalDate.parse(appointment.getDate()), appointment.getTime());
        appointment.setReturnReason("");
        appointment.setCancelledByPatient(false);
        appointment.resetForwarded();
        appointment.setStatus(PENDING);
    }

    /** Null means a nurse can reconsider this returned request. */
    public String reconsiderByNurseProblem(Appointment appointment) {
        if (appointment == null || !REJECTED.equalsIgnoreCase(appointment.getStatus())) {
            return "Only returned (rejected) requests can be reconsidered.";
        }
        if (appointment.isForwardedToDoctor()) {
            return "The doctor already declined this one, so it cannot be reconsidered here.";
        }
        return reviveProblem(appointment);
    }

    /** Nurse changes her mind about a request she returned: it goes back to "Pending" for review. */
    public boolean reconsiderByNurse(Appointment appointment) {
        if (reconsiderByNurseProblem(appointment) != null) {
            return false;
        }
        revive(appointment);

        notifications.notifyByName(appointment.getPatientName(),
                "Your request is being reconsidered",
                "A nurse is looking at your request with " + appointment.getDoctorName() + " on "
                        + appointment.getDate() + " at " + appointment.getTime()
                        + " again. The time is reserved for you while it is reviewed.",
                Notification.Type.INFO);
        return true;
    }

    /** Null means the patient can reconsider this cancelled appointment. */
    public String reconsiderByPatientProblem(Appointment appointment, String patientName) {
        if (appointment == null || patientName == null || appointment.getPatientName() == null
                || !appointment.getPatientName().equalsIgnoreCase(patientName)) {
            return "You can only reconsider your own appointments.";
        }
        if (!CANCELLED.equalsIgnoreCase(appointment.getStatus()) || !appointment.isCancelledByPatient()) {
            return "Only appointments you cancelled can be reconsidered.";
        }
        return reviveProblem(appointment);
    }

    /** Patient changes their mind: the cancelled appointment returns to the nurse for review. */
    public boolean reconsiderByPatient(Appointment appointment, String patientName) {
        if (reconsiderByPatientProblem(appointment, patientName) != null) {
            return false;
        }
        revive(appointment);

        notifications.notifyRole(Nurse.class,
                "Cancelled request reconsidered",
                appointment.getPatientName() + " wants to keep the request with "
                        + appointment.getDoctorName() + " on " + appointment.getDate() + " at "
                        + appointment.getTime() + ". Please review it again.",
                appointment.isPriority() ? Notification.Type.WARNING : Notification.Type.INFO);
        return true;
    }

    /**
     * The patient clears a cancelled or returned request from their own list. The clinic's
     * records keep it; only the patient's view changes.
     */
    public boolean removeByPatient(Appointment appointment, String patientName) {
        if (appointment == null || patientName == null || appointment.getPatientName() == null
                || !appointment.getPatientName().equalsIgnoreCase(patientName)) {
            return false;
        }
        if (!CANCELLED.equalsIgnoreCase(appointment.getStatus())
                && !REJECTED.equalsIgnoreCase(appointment.getStatus())) {
            return false;
        }
        appointment.setHiddenFromPatient(true);
        return true;
    }

    /**
     * Doctor closes the loop after the visit: only the assigned doctor can mark
     * a CONFIRMED appointment as COMPLETED. It is done the moment the doctor presses the button.
     */
    public boolean completeByDoctor(Appointment appointment, String doctorName) {
        if (appointment == null || doctorName == null
                || appointment.getDoctorName() == null
                || !appointment.getDoctorName().equalsIgnoreCase(doctorName)
                || !CONFIRMED.equalsIgnoreCase(appointment.getStatus())) {
            return false;
        }

        appointment.setStatus(COMPLETED);

        notifications.notifyByName(appointment.getPatientName(),
                "Appointment completed",
                "Your appointment with " + appointment.getDoctorName() + " on "
                        + appointment.getDate() + " is marked as completed. How did it go? "
                        + "Open Appointments, select it and press Give Feedback.",
                Notification.Type.SUCCESS);

        return true;
    }

    /**
     * Secretary can pull back an appointment she already confirmed (for example
     * the doctor is unavailable). Frees the slot and tells the patient and doctor.
     */
    public boolean cancelBySecretary(Appointment appointment) {
        if (appointment == null || !CONFIRMED.equalsIgnoreCase(appointment.getStatus())) {
            return false;
        }

        try {
            if (ScheduleService.hasPassed(LocalDate.parse(appointment.getDate()),
                    appointment.getTime())) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }

        appointment.setStatus(CANCELLED);
        restoreSlot(appointment);

        notifications.notifyByName(appointment.getPatientName(),
                "Appointment cancelled by the clinic",
                "Your appointment with " + appointment.getDoctorName() + " on "
                        + appointment.getDate() + " at " + appointment.getTime()
                        + " was cancelled. Please book another time.",
                Notification.Type.ALERT);

        notifications.notifyByName(appointment.getDoctorName(),
                "Confirmed appointment cancelled",
                appointment.getPatientName() + " on " + appointment.getDate()
                        + " at " + appointment.getTime() + " was cancelled by the secretary.",
                Notification.Type.WARNING);

        return true;
    }

    /**
     * Used when a doctor account is deleted or deactivated: every upcoming
     * request or booking with that doctor is cancelled so no patient is left
     * waiting for someone who can no longer see them. Returns how many.
     */
    public int cancelUpcomingForDoctor(String doctorName) {
        if (doctorName == null) return 0;

        int cancelled = 0;

        for (Appointment a : new ArrayList<>(DataStore.appointments)) {
            if (a.getDoctorName() == null || !a.getDoctorName().equalsIgnoreCase(doctorName)) {
                continue;
            }

            String status = a.getStatus();
            if (!(PENDING.equalsIgnoreCase(status) || FOR_DOCTOR_REVIEW.equalsIgnoreCase(status)
                    || DOCTOR_APPROVED.equalsIgnoreCase(status)
                    || CONFIRMED.equalsIgnoreCase(status))) {
                continue;
            }

            try {
                if (ScheduleService.hasPassed(LocalDate.parse(a.getDate()), a.getTime())) {
                    continue;
                }
            } catch (RuntimeException e) {
                continue;
            }

            a.setStatus(CANCELLED);
            cancelled++;

            notifications.notifyByName(a.getPatientName(),
                    "Appointment cancelled",
                    doctorName + " is no longer available. Your appointment on "
                            + a.getDate() + " at " + a.getTime()
                            + " was cancelled. Please book another doctor.",
                    Notification.Type.ALERT);
        }

        return cancelled;
    }

    // ------------------------------------------------------------ nurse support

    /**
     * The nurse checks the vitals the patient reported, or enters them herself
     * (for example after calling a patient who has no home device). Allowed
     * while the request is still active. Once the doctor is involved, the
     * doctor is told about the update.
     */
    public boolean recordVitals(Appointment appointment, VitalSigns vitals) {
        if (appointment == null || vitals == null) {
            return false;
        }

        String status = appointment.getStatus();
        boolean active = PENDING.equalsIgnoreCase(status) || FOR_DOCTOR_REVIEW.equalsIgnoreCase(status)
                || DOCTOR_APPROVED.equalsIgnoreCase(status) || CONFIRMED.equalsIgnoreCase(status);

        if (!active) {
            return false;
        }

        boolean visitWasPending = appointment.hasVitalsCheck() && !appointment.isVitalsChecked();

        appointment.setVitals(vitals);
        appointment.setVitalsChecked(true);

        if (visitWasPending) {
            notifications.notifyByName(appointment.getPatientName(),
                    "Vitals recorded",
                    "Thank you for coming in. The nurse recorded your vitals and will now send your "
                            + "request to " + appointment.getDoctorName() + ".",
                    Notification.Type.SUCCESS);
        }

        if (!PENDING.equalsIgnoreCase(status)) {
            notifications.notifyByName(appointment.getDoctorName(),
                    "Vitals updated by the nurse",
                    "Vitals for " + appointment.getPatientName() + " (" + appointment.getDate()
                            + " " + appointment.getTime() + "): " + vitals.summary(),
                    Notification.Type.INFO);
        }

        return true;
    }

    /**
     * Vitals the nurse already took for this patient and doctor on that day,
     * so a completed consultation does not ask for them a second time.
     */
    public VitalSigns findVitals(String patientName, String doctorName, String date) {
        for (Appointment a : DataStore.appointments) {
            if (a.getVitals() != null
                    && a.getPatientName() != null && a.getPatientName().equalsIgnoreCase(patientName)
                    && a.getDoctorName() != null && a.getDoctorName().equalsIgnoreCase(doctorName)
                    && a.getDate() != null && a.getDate().equals(date)
                    && (CONFIRMED.equalsIgnoreCase(a.getStatus())
                    || COMPLETED.equalsIgnoreCase(a.getStatus()))) {
                return a.getVitals();
            }
        }
        return null;
    }

    // ------------------------------------------------- vitals check-in (clinic visit)

    /** The check-in must leave the nurse this long to forward the request before the appointment. */
    public static final int VITALS_VISIT_LEAD_MINUTES = 60;

    private static final LocalTime VISIT_FIRST = LocalTime.of(8, 0);
    private static final LocalTime VISIT_LAST = LocalTime.of(16, 30);
    private static final int VISIT_STEP_MINUTES = 30;

    private static final DateTimeFormatter VISIT_DAY =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

    /**
     * A patient with no thermometer or BP monitor at home can be asked to come to the clinic
     * and have the nurse take the vitals before the appointment. Returns why that cannot be
     * arranged for this request, or null when it can.
     */
    public String vitalsVisitProblem(Appointment appointment) {
        if (appointment == null) {
            return "Select an appointment first.";
        }
        if (!PENDING.equalsIgnoreCase(appointment.getStatus())) {
            return "A vitals visit can only be arranged while the request is waiting for nurse review.";
        }
        if (appointment.isVitalsChecked()) {
            return "A nurse has already recorded this patient's vitals.";
        }
        if (appointmentStart(appointment) == null) {
            return "This request has an unreadable date or time.";
        }
        return null;
    }

    /** Same, for one chosen date and time. Null means the choice is allowed. */
    public String vitalsVisitProblem(Appointment appointment, LocalDate date, String time) {
        String problem = vitalsVisitProblem(appointment);
        if (problem != null) {
            return problem;
        }

        LocalTime clock = Schedule.toLocalTime(time);
        if (date == null || clock == null) {
            return "Choose a date and a time for the check-in.";
        }

        LocalDateTime visit = LocalDateTime.of(date, clock);

        if (!visit.isAfter(LocalDateTime.now())) {
            return "That time has already passed. Choose a later time.";
        }

        if (visit.isAfter(appointmentStart(appointment).minusMinutes(VITALS_VISIT_LEAD_MINUTES))) {
            return "The check-in must be at least " + VITALS_VISIT_LEAD_MINUTES
                    + " minutes before the appointment (" + appointment.getDate() + " "
                    + appointment.getTime() + ").";
        }

        return null;
    }

    /** Clinic check-in times (every 30 minutes, 8:00 AM to 4:30 PM) that are allowed on that date. */
    public ArrayList<String> vitalsVisitTimes(Appointment appointment, LocalDate date) {
        ArrayList<String> times = new ArrayList<>();

        for (LocalTime t = VISIT_FIRST; !t.isAfter(VISIT_LAST); t = t.plusMinutes(VISIT_STEP_MINUTES)) {
            String text = t.format(TIME_FORMAT);
            if (vitalsVisitProblem(appointment, date, text) == null) {
                times.add(text);
            }
        }

        return times;
    }

    /** The first day from today until the appointment that still has a free check-in time, or null. */
    public LocalDate firstVitalsVisitDate(Appointment appointment) {
        if (vitalsVisitProblem(appointment) != null) {
            return null;
        }

        LocalDate last = appointmentStart(appointment).toLocalDate();

        for (LocalDate d = LocalDate.now(); !d.isAfter(last); d = d.plusDays(1)) {
            if (!vitalsVisitTimes(appointment, d).isEmpty()) {
                return d;
            }
        }

        return null;
    }

    /**
     * The nurse asks the patient to come to the clinic for a vitals check at a set time before
     * the appointment. Calling it again moves the check-in (for example after a missed visit).
     * The doctor's slot stays held and the patient is told exactly when and where to come.
     */
    public boolean scheduleVitalsCheck(Appointment appointment, LocalDate date, String time, String note) {
        if (vitalsVisitProblem(appointment, date, time) != null) {
            return false;
        }

        boolean moved = appointment.hasVitalsCheck();
        String where = isBlank(note) ? "the clinic nurse station" : note.trim();

        appointment.setVitalsCheck(date.toString(), time, note);

        notifications.notifyByName(appointment.getPatientName(),
                moved ? "Vitals check moved" : "Please come in for a vitals check",
                "Come to " + where + " on " + date.format(VISIT_DAY) + " at " + time
                        + " so a nurse can take your vitals before your appointment with "
                        + appointment.getDoctorName() + " on " + appointment.getDate() + " at "
                        + appointment.getTime() + ". Your appointment time stays reserved. "
                        + "If you cannot come, cancel the appointment so someone else can use the slot.",
                Notification.Type.WARNING);

        return true;
    }

    /** What the tables show in the "Vitals Check-in" column. Empty when no visit was arranged. */
    public static String vitalsCheckText(Appointment appointment) {
        if (appointment == null || !appointment.hasVitalsCheck()
                || REJECTED.equalsIgnoreCase(appointment.getStatus())
                || CANCELLED.equalsIgnoreCase(appointment.getStatus())) {
            return "";
        }

        String when;
        boolean passed;
        try {
            LocalDate date = LocalDate.parse(appointment.getVitalsCheckDate());
            when = date.format(VISIT_DAY) + ", " + appointment.getVitalsCheckTime();
            passed = ScheduleService.hasPassed(date, appointment.getVitalsCheckTime());
        } catch (RuntimeException e) {
            return "";
        }

        if (appointment.isVitalsChecked()) {
            return when + " \u2713 done";
        }

        return when + (passed ? "  \u2022 not done yet" : "  \u2022 scheduled");
    }

    /** When the appointment itself starts, or null if its date or time cannot be read. */
    private static LocalDateTime appointmentStart(Appointment appointment) {
        try {
            LocalTime clock = Schedule.toLocalTime(appointment.getTime());
            return clock == null ? null
                    : LocalDateTime.of(LocalDate.parse(appointment.getDate()), clock);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---------------------------------------------------------- doctor support

    /**
     * One line the doctor reads while deciding on a request: how many visits
     * this patient already had with the doctor, cancellations, the latest
     * diagnosis and the latest vitals on file.
     */
    public String historySummary(String patientName, String doctorName, Appointment current) {
        int completed = 0;
        int cancelled = 0;

        for (Appointment a : DataStore.appointments) {
            if (a == current || a.getPatientName() == null || a.getDoctorName() == null
                    || !a.getPatientName().equalsIgnoreCase(patientName)
                    || !a.getDoctorName().equalsIgnoreCase(doctorName)) {
                continue;
            }
            if (COMPLETED.equalsIgnoreCase(a.getStatus())) completed++;
            if (CANCELLED.equalsIgnoreCase(a.getStatus())) cancelled++;
        }

        MedicalRecord latest = null;
        MedicalRecord latestWithVitals = null;

        for (MedicalRecord r : DataStore.medicalRecords) {
            if (!r.getPatientName().equalsIgnoreCase(patientName)
                    || !r.getDoctorName().equalsIgnoreCase(doctorName)) {
                continue;
            }
            if (latest == null || r.getDate().compareTo(latest.getDate()) >= 0) latest = r;
            if (r.getVitalSigns() != null && (latestWithVitals == null
                    || r.getDate().compareTo(latestWithVitals.getDate()) >= 0)) {
                latestWithVitals = r;
            }
        }

        if (completed == 0 && cancelled == 0 && latest == null) {
            return "First visit with you: no earlier appointments or records.";
        }

        StringBuilder text = new StringBuilder("History with you: ")
                .append(completed).append(" completed visit(s)");

        if (cancelled > 0) text.append(", ").append(cancelled).append(" cancelled");

        if (latest != null) {
            text.append(" \u2022 Last diagnosis: ").append(latest.getDiagnosis())
                    .append(" (").append(latest.getDate()).append(")");
        }

        if (latestWithVitals != null) {
            text.append(" \u2022 Latest vitals: ")
                    .append(latestWithVitals.getVitalSigns().summary());
        }

        return text.toString();
    }

    // ------------------------------------------------------------------ sorting

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private static LocalTime timeOf(String time) {
        try {
            return LocalTime.parse(time.trim().toUpperCase(Locale.ENGLISH), TIME_FORMAT);
        } catch (RuntimeException e) {
            return LocalTime.MAX;
        }
    }

    /** Earliest date first, then earliest clock time ("01:00 PM" after "09:00 AM"). */
    public static final Comparator<Appointment> BY_SCHEDULE = (x, y) -> {
        int byDate = String.valueOf(x.getDate()).compareTo(String.valueOf(y.getDate()));
        return byDate != 0 ? byDate : timeOf(x.getTime()).compareTo(timeOf(y.getTime()));
    };

    /** Priority (senior / PWD) patients before regular ones, then the one who asked first. */
    public static final Comparator<Appointment> PRIORITY_THEN_ARRIVAL = (x, y) -> {
        if (x.isPriority() != y.isPriority()) return x.isPriority() ? -1 : 1;
        return Integer.compare(x.getId(), y.getId());
    };

    /** Same priority rule for tables that are sorted by date and time. */
    private static int byPriority(Appointment x, Appointment y) {
        if (x.isPriority() != y.isPriority()) return x.isPriority() ? -1 : 1;
        return 0;
    }

    /** Secretary view: requests that need her confirmation come first, priority patients first among them. */
    public static final Comparator<Appointment> SECRETARY_FIRST = (x, y) -> {
        boolean xi = DOCTOR_APPROVED.equalsIgnoreCase(x.getStatus());
        boolean yi = DOCTOR_APPROVED.equalsIgnoreCase(y.getStatus());
        if (xi != yi) return xi ? -1 : 1;
        int byType = xi ? byPriority(x, y) : 0;
        return byType != 0 ? byType : BY_SCHEDULE.compare(x, y);
    };

    /** Nurse view: new requests that need her review come first, priority patients first among them. */
    public static final Comparator<Appointment> NURSE_FIRST = (x, y) -> {
        boolean xi = PENDING.equalsIgnoreCase(x.getStatus());
        boolean yi = PENDING.equalsIgnoreCase(y.getStatus());
        if (xi != yi) return xi ? -1 : 1;
        int byType = xi ? byPriority(x, y) : 0;
        return byType != 0 ? byType : BY_SCHEDULE.compare(x, y);
    };

    /** Doctor view: requests waiting for the doctor come first, priority patients first among them. */
    public static final Comparator<Appointment> DOCTOR_FIRST = (x, y) -> {
        boolean xi = FOR_DOCTOR_REVIEW.equalsIgnoreCase(x.getStatus());
        boolean yi = FOR_DOCTOR_REVIEW.equalsIgnoreCase(y.getStatus());
        if (xi != yi) return xi ? -1 : 1;
        int byType = xi ? byPriority(x, y) : 0;
        return byType != 0 ? byType : BY_SCHEDULE.compare(x, y);
    };

    private void restoreSlot(Appointment appointment) {
        try {
            new ScheduleService().restoreTime(appointment.getDoctorName(),
                    LocalDate.parse(appointment.getDate()), appointment.getTime());
        } catch (RuntimeException ignored) {
            // Old record with an unreadable date: the status change still stands.
        }
    }

    /** A request keeps its slot until it is rejected or cancelled. */
    private boolean holdsSlot(Appointment a) {
        return a != null
                && !REJECTED.equalsIgnoreCase(a.getStatus())
                && !CANCELLED.equalsIgnoreCase(a.getStatus());
    }

    private boolean sameSlot(Appointment a, Appointment b) {
        return a != null
                && Objects.equals(normalize(a.getDoctorName()), normalize(b.getDoctorName()))
                && Objects.equals(a.getDate(), b.getDate())
                && Objects.equals(a.getTime(), b.getTime());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
