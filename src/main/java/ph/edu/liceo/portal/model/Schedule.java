package ph.edu.liceo.portal.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * In-memory doctor availability calendar.
 *
 * A date is "open" when the doctor has switched it on. It is only available to
 * patients when it is open AND still has at least one free time slot, so a day
 * whose slots are all booked turns red without the doctor's own setting being
 * overwritten. Booked slots are remembered separately so they can never come
 * back by toggling a day, and so a rejected request restores exactly its slot.
 */
public class Schedule {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private String doctorName;
    private final Map<LocalDate, Boolean> open;
    private final Map<LocalDate, ArrayList<String>> availableTimes;
    private final Map<LocalDate, Set<String>> bookedTimes;
    private LocalDate seededThrough;

    public Schedule(String doctorName) {
        this.doctorName = doctorName;
        this.open = new LinkedHashMap<>();
        this.availableTimes = new LinkedHashMap<>();
        this.bookedTimes = new LinkedHashMap<>();
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    // ------------------------------------------------------------ seeding

    public LocalDate getSeededThrough() {
        return seededThrough;
    }

    public void setSeededThrough(LocalDate seededThrough) {
        this.seededThrough = seededThrough;
    }

    /** Whether the date has been given any setting yet (default or doctor-made). */
    public boolean hasDate(LocalDate date) {
        return open.containsKey(date);
    }

    // ------------------------------------------------------------ queries

    public ArrayList<String> getAvailableDates() {
        ArrayList<String> dates = new ArrayList<>();

        for (LocalDate date : open.keySet()) {
            if (isAvailable(date)) {
                dates.add(date.toString());
            }
        }

        return dates;
    }

    public ArrayList<String> getAvailableTimes() {
        ArrayList<String> result = new ArrayList<>();

        for (LocalDate date : open.keySet()) {
            for (String time : getTimes(date)) {
                if (!result.contains(time)) {
                    result.add(time);
                }
            }
        }

        result.sort(timeOrder());
        return result;
    }

    /** The doctor has switched this date on (it may still be fully booked). */
    public boolean isOpen(LocalDate date) {
        return Boolean.TRUE.equals(open.get(date));
    }

    /** Open and with at least one free slot. */
    public boolean isAvailable(LocalDate date) {
        return isOpen(date) && !freeTimes(date).isEmpty();
    }

    public ArrayList<String> getTimes(LocalDate date) {
        if (!isOpen(date)) {
            return new ArrayList<>();
        }

        return freeTimes(date);
    }

    public boolean hasTime(LocalDate date, String time) {
        return isOpen(date) && freeTimes(date).contains(time);
    }

    public boolean isBooked(LocalDate date, String time) {
        return bookedTimes.getOrDefault(date, Set.of()).contains(time);
    }

    // ------------------------------------------------------------ doctor edits

    public void addDate(String date) {
        setAvailable(LocalDate.parse(date), true);
    }

    public void addTime(String time) {
        for (LocalDate date : new ArrayList<>(open.keySet())) {
            addTime(date, time);
        }
    }

    public void setAvailable(LocalDate date, boolean value) {
        open.put(date, value);

        if (value) {
            ArrayList<String> times = availableTimes.get(date);

            // Reuse the doctor's own times if they still have some;
            // otherwise start from the defaults. Booked slots never return.
            if (times == null || times.isEmpty()) {
                availableTimes.put(date, defaultTimes());
            }

            removeBooked(date);
            sort(date);
        }
    }

    public void toggle(LocalDate date) {
        setAvailable(date, !isOpen(date));
    }

    public void setTimes(LocalDate date, List<String> times) {
        open.put(date, true);
        availableTimes.put(date, new ArrayList<>(times));
        removeBooked(date);
        sort(date);
    }

    public void addTime(LocalDate date, String time) {
        if (!open.containsKey(date) || !isOpen(date)) {
            open.put(date, true);
            availableTimes.computeIfAbsent(date, ignored -> new ArrayList<>());
        }

        // A slot that is already booked cannot be offered again.
        if (isBooked(date, time)) {
            return;
        }

        ArrayList<String> times = availableTimes.computeIfAbsent(
                date, ignored -> new ArrayList<>());

        if (!times.contains(time)) {
            times.add(time);
            sort(date);
        }
    }

    public void removeTime(LocalDate date, String time) {
        ArrayList<String> times = availableTimes.get(date);

        if (times != null) {
            times.remove(time);
        }
    }

    // ------------------------------------------------------------ booking

    public void bookTime(LocalDate date, String time) {
        removeTime(date, time);
        bookedTimes.computeIfAbsent(date, ignored -> new HashSet<>()).add(time);
    }

    /** Gives back exactly one slot (a rejected request) without reopening a blocked day. */
    public void restoreTime(LocalDate date, String time) {
        if (date == null || time == null || time.isBlank()) {
            return;
        }

        Set<String> booked = bookedTimes.get(date);
        if (booked != null) {
            booked.remove(time);
        }

        if (!open.containsKey(date)) {
            return;
        }

        ArrayList<String> times = availableTimes.computeIfAbsent(
                date, ignored -> new ArrayList<>());

        if (!times.contains(time)) {
            times.add(time);
            sort(date);
        }
    }

    // ------------------------------------------------------------ helpers

    private ArrayList<String> freeTimes(LocalDate date) {
        ArrayList<String> free = new ArrayList<>();
        Set<String> booked = bookedTimes.getOrDefault(date, Set.of());

        for (String time : availableTimes.getOrDefault(date, new ArrayList<>())) {
            if (!booked.contains(time)) {
                free.add(time);
            }
        }

        return free;
    }

    private void removeBooked(LocalDate date) {
        ArrayList<String> times = availableTimes.get(date);
        Set<String> booked = bookedTimes.get(date);

        if (times != null && booked != null) {
            times.removeAll(booked);
        }
    }

    private void sort(LocalDate date) {
        ArrayList<String> times = availableTimes.get(date);

        if (times != null) {
            times.sort(timeOrder());
        }
    }

    private Comparator<String> timeOrder() {
        return Comparator.comparing(Schedule::parseTime,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time.trim().toUpperCase(Locale.ENGLISH), TIME_FORMAT);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Parses a slot such as "09:00 AM"; null if it cannot be read. */
    public static LocalTime toLocalTime(String time) {
        return time == null ? null : parseTime(time);
    }

    private ArrayList<String> defaultTimes() {
        ArrayList<String> times = new ArrayList<>();
        times.add("09:00 AM");
        times.add("10:00 AM");
        times.add("11:00 AM");
        times.add("01:00 PM");
        times.add("02:00 PM");
        times.add("03:00 PM");
        times.add("04:00 PM");
        return times;
    }
}
