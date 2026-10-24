package ar.edu.um.turnos.appointment.availability.domain;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentSlot;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Pure domain function: expands enabled weekly schedules into appointment start/end slots. */
public final class WeeklyScheduleSlotGenerator {

    private WeeklyScheduleSlotGenerator() {
    }

    public static List<AppointmentSlot> generate(LocalDate date, List<WeeklySchedule> schedules) {
        Objects.requireNonNull(date, "Date is required.");
        Objects.requireNonNull(schedules, "Schedules are required.");

        return schedules.stream()
                .filter(schedule -> schedule.dayOfWeek() == date.getDayOfWeek())
                .flatMap(schedule -> generate(date, schedule).stream())
                .distinct()
                .sorted(Comparator.comparing(AppointmentSlot::startTime)
                        .thenComparing(AppointmentSlot::endTime))
                .toList();
    }

    private static List<AppointmentSlot> generate(LocalDate date, WeeklySchedule schedule) {
        List<AppointmentSlot> slots = new java.util.ArrayList<>();
        LocalTime start = schedule.startTime();
        while (Duration.between(start, schedule.endTime()).toMinutes() >= schedule.slotDurationMinutes()) {
            LocalTime end = start.plusMinutes(schedule.slotDurationMinutes());
            slots.add(new AppointmentSlot(date, start, end));
            start = end;
        }
        return slots;
    }
}
