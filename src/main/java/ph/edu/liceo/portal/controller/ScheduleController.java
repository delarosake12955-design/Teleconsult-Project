package ph.edu.liceo.portal.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import ph.edu.liceo.portal.data.DataStore;
import ph.edu.liceo.portal.model.Doctor;
import ph.edu.liceo.portal.model.Schedule;
import ph.edu.liceo.portal.model.User;
import ph.edu.liceo.portal.service.ScheduleService;

public class ScheduleController {

    @FXML private GridPane calendarGrid;
    @FXML private Label monthLabel;
    @FXML private Label selectedDateLabel;
    @FXML private Label availabilityLabel;
    @FXML private ComboBox<String> timeComboBox;
    @FXML private Label messageLabel;
    @FXML private VBox timePanel;

    private final ScheduleService scheduleService = new ScheduleService();
    private Doctor currentDoctor;
    private YearMonth displayedMonth = YearMonth.now();
    private LocalDate selectedDate;

    @FXML
    private void initialize() {
        User user = LoginController.getLoggedInUser();

        if (user instanceof Doctor doctor) {
            currentDoctor = doctor;
        }

        if (currentDoctor == null) {
            availabilityLabel.setText("Doctor account required");
            return;
        }

        selectedDate = LocalDate.now();
        selectedDateLabel.setText(formatDate(selectedDate));
        loadCalendar();
        loadSelectedDate();
    }

    @FXML
    private void handlePreviousMonth() {
        displayedMonth = displayedMonth.minusMonths(1);
        loadCalendar();
    }

    @FXML
    private void handleNextMonth() {
        displayedMonth = displayedMonth.plusMonths(1);
        loadCalendar();
    }

    @FXML
    private void handleToday() {
        displayedMonth = YearMonth.now();
        selectedDate = LocalDate.now();
        selectedDateLabel.setText(formatDate(selectedDate));
        loadCalendar();
        loadSelectedDate();
    }

    @FXML
    private void handleToggleAvailability() {
        if (selectedDate == null || currentDoctor == null) {
            return;
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            messageLabel.setText("Past dates can't be changed.");
            return;
        }

        scheduleService.toggleDate(
                currentDoctor.getFullName(),
                selectedDate
        );

        loadCalendar();
        loadSelectedDate();
        messageLabel.setText(
                scheduleService.getSchedule(currentDoctor.getFullName())
                        .isOpen(selectedDate)
                        ? "This date is now open for booking."
                        : "This date is now blocked."
        );
    }

    @FXML
    private void handleAddTime() {
        if (selectedDate == null || currentDoctor == null) {
            return;
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            messageLabel.setText("Past dates can't be changed.");
            return;
        }

        String time = timeComboBox.getValue();

        if (time == null || time.isBlank()) {
            messageLabel.setText("Select a time first.");
            return;
        }

        scheduleService.addTime(
                currentDoctor.getFullName(),
                selectedDate,
                time
        );

        scheduleService.setAvailable(
                currentDoctor.getFullName(),
                selectedDate,
                true
        );

        loadCalendar();
        loadSelectedDate();
        messageLabel.setText(time + " added to the selected date.");
    }

    @FXML
    private void handleRemoveTime() {
        if (selectedDate == null || currentDoctor == null) {
            return;
        }

        if (selectedDate.isBefore(LocalDate.now())) {
            messageLabel.setText("Past dates can't be changed.");
            return;
        }

        String time = timeComboBox.getValue();

        if (time == null || time.isBlank()) {
            messageLabel.setText("Select a time first.");
            return;
        }

        scheduleService.removeTime(
                currentDoctor.getFullName(),
                selectedDate,
                time
        );

        loadCalendar();
        loadSelectedDate();
        messageLabel.setText(time + " removed from the selected date.");
    }

    private void loadCalendar() {
        calendarGrid.getChildren().clear();

        String[] weekdays = {
                "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"
        };

        for (int column = 0; column < 7; column++) {
            Label header = new Label(weekdays[column]);
            header.getStyleClass().add("calendar-weekday");
            calendarGrid.add(header, column, 0);
        }

        monthLabel.setText(
                displayedMonth.format(
                        DateTimeFormatter.ofPattern("MMMM yyyy")
                )
        );

        LocalDate first = displayedMonth.atDay(1);
        int startColumn = first.getDayOfWeek().getValue() - 1;
        int days = displayedMonth.lengthOfMonth();

        for (int day = 1; day <= days; day++) {
            LocalDate date = displayedMonth.atDay(day);
            int index = startColumn + day - 1;
            int column = index % 7;
            int row = index / 7 + 1;

            Button cell = new Button(String.valueOf(day));
            cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            cell.setPrefHeight(68);
            cell.setMinHeight(68);
            cell.getStyleClass().add("calendar-day");

            if (scheduleService.isAvailable(
                    currentDoctor.getFullName(), date)) {
                cell.getStyleClass().add("calendar-available");
            } else {
                cell.getStyleClass().add("calendar-unavailable");
            }

            if (date.equals(LocalDate.now())) {
                cell.getStyleClass().add("calendar-today");
            }

            if (date.equals(selectedDate)) {
                cell.getStyleClass().add("calendar-selected");
            }

            cell.setOnAction(event -> selectDate(date));
            calendarGrid.add(cell, column, row);
        }
    }

    private void selectDate(LocalDate date) {
        selectedDate = date;
        selectedDateLabel.setText(formatDate(date));
        messageLabel.setText("");
        loadCalendar();
        loadSelectedDate();
    }

    private void loadSelectedDate() {
        if (selectedDate == null || currentDoctor == null) {
            return;
        }

        Schedule schedule = scheduleService.getSchedule(
                currentDoctor.getFullName()
        );

        boolean available = schedule.isAvailable(selectedDate);

        availabilityLabel.setText(
                available ? "AVAILABLE" : "NOT AVAILABLE"
        );

        availabilityLabel.getStyleClass().removeAll(
                "availability-green",
                "availability-red"
        );

        availabilityLabel.getStyleClass().add(
                available ? "availability-green" : "availability-red"
        );

        timeComboBox.getItems().setAll(
                "09:00 AM", "10:00 AM", "11:00 AM",
                "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM"
        );

        // The doctor can edit times on any open future day, even a fully booked one.
        timePanel.setDisable(
                !schedule.isOpen(selectedDate)
                        || selectedDate.isBefore(LocalDate.now())
        );

        ArrayList<String> free = schedule.getTimes(selectedDate);
        messageLabel.setText(free.isEmpty()
                ? ""
                : "Open slots: " + String.join(", ", free));
    }

    private String formatDate(LocalDate date) {
        return date.format(
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        );
    }
}
