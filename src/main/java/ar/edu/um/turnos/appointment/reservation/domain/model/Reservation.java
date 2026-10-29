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
        String reservationProcessId,
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
        PhoneSubmissionStatus phoneSubmissionStatus,
        String phoneSubmissionEventId,
        String status,
        Instant createdAt,
        Instant confirmedAt,
        Instant cancelledAt,
        String cancellationReason) {

    public Reservation withId(Long newId) {
        return new Reservation(newId, processId, reservationId, reservationProcessId, ownerPublicId, externalPatientId,
                professionalId, appointmentDate, startTime, endTime, professionalFirstName,
                professionalLastName, patientFirstName, patientLastName, patientPhone,
                phoneSubmissionStatus, phoneSubmissionEventId, status, createdAt,
                confirmedAt, cancelledAt, cancellationReason);
    }

    public Reservation phoneSubmissionPending(String phone, String eventId) {
        if (phoneSubmissionStatus != PhoneSubmissionStatus.NOT_SENT) {
            throw new IllegalStateException("A phone submission is already pending or was sent.");
        }
        return new Reservation(id, processId, reservationId, reservationProcessId, ownerPublicId, externalPatientId,
                professionalId, appointmentDate, startTime, endTime, professionalFirstName,
                professionalLastName, patientFirstName, patientLastName, phone,
                PhoneSubmissionStatus.PENDING, eventId, status, createdAt, confirmedAt,
                cancelledAt, cancellationReason);
    }

    public Reservation phoneSubmissionSent(String eventId) {
        if (phoneSubmissionStatus != PhoneSubmissionStatus.PENDING
                || phoneSubmissionEventId == null || !phoneSubmissionEventId.equals(eventId)) {
            throw new IllegalStateException("Only the matching pending phone event can be marked as sent.");
        }
        return new Reservation(id, processId, reservationId, reservationProcessId, ownerPublicId, externalPatientId,
                professionalId, appointmentDate, startTime, endTime, professionalFirstName,
                professionalLastName, patientFirstName, patientLastName, patientPhone,
                PhoneSubmissionStatus.SENT, phoneSubmissionEventId, status, createdAt,
                confirmedAt, cancelledAt, cancellationReason);
    }

    public Reservation withEvent(ReservationEvent event, String newStatus, Instant now) {
        boolean newPhoneRequest = event != null && (
                "AdditionalInformationRequested".equals(event.eventType())
                        || "AdditionalInformationRejected".equals(event.eventType()));
        return new Reservation(id, processId,
                event == null || event.reservationId() == null ? reservationId : event.reservationId(),
                reservationProcessId, ownerPublicId,
                externalPatientId, professionalId, appointmentDate, startTime, endTime,
                professionalFirstName, professionalLastName, patientFirstName, patientLastName,
                newPhoneRequest ? null : patientPhone,
                newPhoneRequest ? PhoneSubmissionStatus.NOT_SENT : phoneSubmissionStatus,
                newPhoneRequest ? null : phoneSubmissionEventId, newStatus, createdAt,
                event != null && "AppointmentConfirmed".equals(event.eventType())
                        ? (event.occurredAt() == null ? now : event.occurredAt()) : confirmedAt,
                cancelledAt, cancellationReason);
    }
}
