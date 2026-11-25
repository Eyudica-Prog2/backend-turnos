package ar.edu.um.turnos.appointment.availability.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record AppointmentOccupancy(
        LocalDate date,
        LocalTime startTime,
        OccupancyStatus status,
        Instant expiresAt) {

    public AppointmentOccupancy {
        Objects.requireNonNull(date, "Occupancy date is required.");
        Objects.requireNonNull(startTime, "Occupancy start time is required.");
        Objects.requireNonNull(status, "Occupancy status is required.");
        if (status == OccupancyStatus.HELD && expiresAt == null) {
            throw new IllegalArgumentException("A held occupancy must have an expiration time.");
        }
        if (status == OccupancyStatus.CONFIRMED && expiresAt != null) {
            throw new IllegalArgumentException("A confirmed occupancy cannot have an expiration time.");
        }
    }
}
