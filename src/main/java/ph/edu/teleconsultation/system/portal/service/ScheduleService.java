package ph.edu.teleconsultation.system.portal.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

import ph.edu.teleconsultation.system.portal.data.DataStore;
import ph.edu.teleconsultation.system.portal.model.Schedule;

public class ScheduleService {

    /** How far ahead the default Monday-Friday calendar is kept filled in. */
    private static final int HORIZON_DAYS = 120;

    public Schedule getSchedule(String doctorName) {
        Schedule schedule = DataStore.scheduleData.getOrCreate(doctorName);
        ensureDefaultSchedule(schedule);
        return schedule;
    }

    /** Doctor-side view: the doctor's own setting for a date, past or future. */
    public boolean isAvailable(String doctorName, LocalDate date) {
        return getSchedule(doctorName).isAvailable(date);
    }

    public ArrayList<String> getAvailableTimes(String doctorName, LocalDate date) {
        return getSchedule(doctorName).getTimes(date);
    }

    /** Patient-side view: a date can only be booked today or later. */
    public boolean isBookable(String doctorName, LocalDate date) {
        return date != null
                && !date.isBefore(LocalDate.now())
                && !getBookableTimes(doctorName, date).isEmpty();
    }

    /** Free slots for a date, leaving out hours that have already passed today. */
    public ArrayList<String> getBookableTimes(String doctorName, LocalDate date) {
        ArrayList<String> result = new ArrayList<>();

        if (date == null || date.isBefore(LocalDate.now())) {
            return result;
        }

        for (String time : getAvailableTimes(doctorName, date)) {
            if (!hasPassed(date, time)) {
                result.add(time);
            }
        }

        return result;
    }

    public static boolean hasPassed(LocalDate date, String time) {
        if (date == null) {
            return true;
        }

        if (date.isBefore(LocalDate.now())) {
            return true;
        }

        if (date.isAfter(LocalDate.now())) {
            return false;
        }

        LocalTime parsed = Schedule.toLocalTime(time);
        return parsed != null && !parsed.isAfter(LocalTime.now());
    }

    public void toggleDate(String doctorName, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            return;
        }
        getSchedule(doctorName).toggle(date);
    }

    public void setAvailable(String doctorName, LocalDate date, boolean available) {
        if (date.isBefore(LocalDate.now())) {
            return;
        }
        getSchedule(doctorName).setAvailable(date, available);
    }

    public void addTime(String doctorName, LocalDate date, String time) {
        if (date.isBefore(LocalDate.now())) {
            return;
        }
        getSchedule(doctorName).addTime(date, time);
    }

    public void removeTime(String doctorName, LocalDate date, String time) {
        if (date.isBefore(LocalDate.now())) {
            return;
        }
        getSchedule(doctorName).removeTime(date, time);
    }

    public void bookTime(String doctorName, LocalDate date, String time) {
        getSchedule(doctorName).bookTime(date, time);
    }

    public void restoreTime(String doctorName, LocalDate date, String time) {
        getSchedule(doctorName).restoreTime(date, time);
    }

    /**
     * Fills in the default working calendar (Mon-Fri open, weekends closed)
     * for any dates in the next {@value #HORIZON_DAYS} days that have no
     * setting yet. Dates the doctor has already changed are never touched, and
     * the calendar is never re-seeded just because every date was blocked.
     */
    private void ensureDefaultSchedule(Schedule schedule) {
        LocalDate today = LocalDate.now();
        LocalDate last = today.plusDays(HORIZON_DAYS);
        LocalDate from = schedule.getSeededThrough() == null
                || schedule.getSeededThrough().isBefore(today)
                ? today
                : schedule.getSeededThrough().plusDays(1);

        for (LocalDate date = from; !date.isAfter(last); date = date.plusDays(1)) {
            if (schedule.hasDate(date)) {
                continue;
            }

            DayOfWeek day = date.getDayOfWeek();
            schedule.setAvailable(date,
                    day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY);
        }

        schedule.setSeededThrough(last);
    }
}
