package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Reservation projection owned by this service, separate from the process lifecycle. */
public record Reservation(
        Long id,
        Long processId,
        Long reservationId,
        UUID ownerPublicId,
        String externalPatientId,
        long professionalId,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String professionalFirstName,
        String professionalLastName,
        String patientFirstName,
        String patientLastName,
        String patientPhone,
        String status,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        String cancellationReason) {

    public Reservation withId(Long newId) {
        return new Reservation(newId, processId, reservationId, ownerPublicId, externalPatientId,
                professionalId, appointmentDate, startTime, endTime, professionalFirstName,
                professionalLastName, patientFirstName, patientLastName, patientPhone,
                status, createdAt, confirmedAt, cancelledAt, cancellationReason);
    }

    public Reservation withEvent(ReservationEvent event, String newStatus, Instant now) {
        return new Reservation(id, processId,
                event == null || event.reservationId() == null ? reservationId : event.reservationId(), ownerPublicId,
                externalPatientId, professionalId, appointmentDate, startTime, endTime,
                professionalFirstName, professionalLastName, patientFirstName, patientLastName,
                patientPhone, newStatus, createdAt,
                event != null && "AppointmentConfirmed".equals(event.eventType())
                        ? (event.occurredAt() == null ? now : event.occurredAt()) : confirmedAt,
                cancelledAt, cancellationReason);
    }
}
