package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/** Local, user-owned state for one appointment hold and its asynchronous process. */
public record ReservationProcess(
        Long id,
        String reservationProcessId,
        String holdId,
        UUID ownerPublicId,
        long professionalId,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String professionalFirstName,
        String professionalLastName,
        String patientFirstName,
        String patientLastName,
        ReservationProcessStatus status,
        Instant expiresAt,
        String requestEventId,
        Long reservationId,
        String resultStatus,
        String lastError,
        String holdRequestPayload,
        String confirmRequestPayload,
        Instant createdAt,
        Instant updatedAt) {

    public ReservationProcess {
        Objects.requireNonNull(ownerPublicId, "Reservation owner is required.");
        Objects.requireNonNull(appointmentDate, "Appointment date is required.");
        Objects.requireNonNull(startTime, "Appointment start time is required.");
        Objects.requireNonNull(endTime, "Appointment end time is required.");
        Objects.requireNonNull(status, "Reservation process status is required.");
        if (professionalId <= 0 || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("A valid professional and appointment slot are required.");
        }
    }

    public static ReservationProcess created(UUID ownerPublicId, long professionalId,
                                             LocalDate date, LocalTime startTime, LocalTime endTime,
                                             String professionalFirstName, String professionalLastName,
                                             String holdRequestPayload, Instant now) {
        return new ReservationProcess(null, null, null, ownerPublicId, professionalId, date,
                startTime, endTime, professionalFirstName, professionalLastName, null, null,
                ReservationProcessStatus.CREATED, null, null, null, null, null,
                holdRequestPayload, null, now, now);
    }

    public ReservationProcess withId(Long newId) {
        return copy(newId, reservationProcessId, holdId, patientFirstName, patientLastName,
                status, expiresAt, requestEventId, reservationId, resultStatus, lastError,
                confirmRequestPayload, updatedAt);
    }

    public ReservationProcess held(String processId, String newHoldId, Instant expiration,
                                   String requestPayload, Instant now) {
        if (status != ReservationProcessStatus.CREATED) {
            throw new IllegalStateException("Only a newly created process can accept a hold.");
        }
        if (isBlank(processId) || isBlank(newHoldId) || expiration == null) {
            throw new IllegalArgumentException("The catedra hold response is incomplete.");
        }
        return new ReservationProcess(id, processId, newHoldId, ownerPublicId, professionalId,
                appointmentDate, startTime, endTime, professionalFirstName, professionalLastName,
                patientFirstName, patientLastName, ReservationProcessStatus.HELD, expiration,
                requestEventId, reservationId, resultStatus, null, requestPayload,
                confirmRequestPayload, createdAt, now);
    }

    public ReservationProcess waitingForPhone(String firstName, String lastName,
                                               String payload, Instant expiration, Instant now) {
        if (status != ReservationProcessStatus.HELD) {
            throw new IllegalStateException("Only a held process can be confirmed initially.");
        }
        return new ReservationProcess(id, reservationProcessId, holdId, ownerPublicId, professionalId,
                appointmentDate, startTime, endTime, professionalFirstName, professionalLastName,
                firstName, lastName, ReservationProcessStatus.WAITING_FOR_PHONE,
                expiration == null ? expiresAt : expiration, requestEventId, reservationId,
                "PHONE_PENDING", null, holdRequestPayload, payload, createdAt, now);
    }

    public ReservationProcess withLastError(String error, Instant now) {
        return copy(id, reservationProcessId, holdId, patientFirstName, patientLastName,
                status, expiresAt, requestEventId, reservationId, resultStatus,
                error, confirmRequestPayload, now);
    }

    public ReservationProcess apply(ReservationEvent event, Instant now) {
        if (status.isFinal()) {
            return this;
        }
        return switch (event.eventType()) {
            case "AdditionalInformationRequested" -> copy(id, reservationProcessId,
                    event.holdId() == null ? holdId : event.holdId(), patientFirstName, patientLastName,
                    ReservationProcessStatus.WAITING_FOR_PHONE,
                    event.expiresAt() == null ? expiresAt : event.expiresAt(),
                    event.eventId(), reservationId, "PHONE_PENDING", null, confirmRequestPayload, now);
            case "AppointmentConfirmed" -> copy(id, reservationProcessId, holdId,
                    patientFirstName, patientLastName, ReservationProcessStatus.CONFIRMED,
                    expiresAt, requestEventId, event.reservationId(), "CONFIRMED", null,
                    confirmRequestPayload, now);
            case "AdditionalInformationRejected" -> copy(id, reservationProcessId, holdId,
                    patientFirstName, patientLastName, ReservationProcessStatus.REJECTED_RETRYABLE,
                    event.expiresAt() == null ? expiresAt : event.expiresAt(), requestEventId,
                    reservationId, event.reason(), event.message(), confirmRequestPayload, now);
            case "AppointmentProcessExpired" -> copy(id, reservationProcessId, holdId,
                    patientFirstName, patientLastName, ReservationProcessStatus.EXPIRED,
                    expiresAt, requestEventId, reservationId, event.reason(), null,
                    confirmRequestPayload, now);
            case "AppointmentProcessInvalid" -> copy(id, reservationProcessId, holdId,
                    patientFirstName, patientLastName, ReservationProcessStatus.INVALID,
                    expiresAt, requestEventId, reservationId, event.reason(), null,
                    confirmRequestPayload, now);
            default -> throw new IllegalArgumentException("Unsupported reservation event type.");
        };
    }

    private ReservationProcess copy(Long newId, String processId, String newHoldId,
                                    String firstName, String lastName, ReservationProcessStatus newStatus,
                                    Instant newExpiresAt, String newRequestEventId, Long newReservationId,
                                    String newResultStatus, String newLastError,
                                    String newConfirmPayload, Instant now) {
        return new ReservationProcess(newId, processId, newHoldId, ownerPublicId, professionalId,
                appointmentDate, startTime, endTime, professionalFirstName, professionalLastName,
                firstName, lastName, newStatus, newExpiresAt, newRequestEventId,
                newReservationId, newResultStatus, newLastError, holdRequestPayload,
                newConfirmPayload, createdAt, now);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
