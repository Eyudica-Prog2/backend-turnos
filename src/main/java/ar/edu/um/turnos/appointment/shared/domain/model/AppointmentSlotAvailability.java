package ar.edu.um.turnos.appointment.shared.domain.model;

import java.time.Instant;
import java.time.LocalTime;

/** Minimal current catalog and occupancy facts needed to create a reservation. */
public record AppointmentSlotAvailability(
        String professionalFirstName,
        String professionalLastName,
        boolean professionalEnabled,
        LocalTime endTime,
        SlotOccupancy occupancy,
        Instant occupiedUntil) {
}
