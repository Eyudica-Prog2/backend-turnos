package ar.edu.um.turnos.appointment.availability.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record AppointmentSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {

    public AppointmentSlot {
        Objects.requireNonNull(date, "Slot date is required.");
        Objects.requireNonNull(startTime, "Slot start time is required.");
        Objects.requireNonNull(endTime, "Slot end time is required.");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Slot start time must be before end time.");
        }
    }
}
