package ph.edu.liceo.portal.data;

import java.time.LocalDate;
import java.util.ArrayList;

import ph.edu.liceo.portal.model.Schedule;

public class ScheduleData {

    private final ArrayList<Schedule> schedules = new ArrayList<>();

    public void addSchedule(Schedule schedule) {
        if (schedule != null && findByDoctor(schedule.getDoctorName()) == null) {
            schedules.add(schedule);
        }
    }

    public Schedule findByDoctor(String doctorName) {
        for (Schedule schedule : schedules) {
            if (schedule.getDoctorName().equalsIgnoreCase(doctorName)) {
                return schedule;
            }
        }
        return null;
    }

    public Schedule getOrCreate(String doctorName) {
        Schedule schedule = findByDoctor(doctorName);

        if (schedule == null) {
            schedule = new Schedule(doctorName);
            schedules.add(schedule);
        }

        return schedule;
    }

    public ArrayList<Schedule> getSchedules() {
        return schedules;
    }

    public boolean isAvailable(String doctorName, LocalDate date) {
        Schedule schedule = findByDoctor(doctorName);
        return schedule != null && schedule.isAvailable(date);
    }
}
