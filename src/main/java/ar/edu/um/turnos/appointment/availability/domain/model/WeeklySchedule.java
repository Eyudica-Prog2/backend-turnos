package ar.edu.um.turnos.appointment.availability.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

public record WeeklySchedule(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes) {

    public WeeklySchedule {
        Objects.requireNonNull(dayOfWeek, "Schedule day is required.");
        Objects.requireNonNull(startTime, "Schedule start time is required.");
        Objects.requireNonNull(endTime, "Schedule end time is required.");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Schedule start time must be before end time.");
        }
        if (slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("Slot duration must be positive.");
        }
    }
}
