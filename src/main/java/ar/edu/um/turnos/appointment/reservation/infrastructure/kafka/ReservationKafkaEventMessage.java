package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
record ReservationKafkaEventMessage(
        String eventId,
        String eventType,
        String groupId,
        String reservationProcessId,
        String holdId,
        String requestEventId,
        String requiredField,
        Long reservationId,
        Long professionalId,
        @JsonAlias("date") String appointmentDate,
        String startTime,
        String status,
        String reason,
        String message,
        Instant expiresAt,
        Instant occurredAt,
        Integer schemaVersion) {

    void validateContract() {
        if (blank(eventId) || blank(eventType) || blank(groupId) || blank(reservationProcessId)
                || blank(holdId) || occurredAt == null || schemaVersion == null || schemaVersion != 1) {
            throw new IllegalArgumentException("The reservation event is incomplete or has an unsupported version.");
        }
        switch (eventType) {
            case "AdditionalInformationRequested" -> {
                if (!"PHONE_NUMBER".equals(requiredField) || blank(message) || expiresAt == null) {
                    throw new IllegalArgumentException("AdditionalInformationRequested is incomplete.");
                }
            }
            case "AppointmentConfirmed" -> {
                if (blank(requestEventId) || reservationId == null || professionalId == null
                        || blank(appointmentDate) || blank(startTime) || !"CONFIRMED".equals(status)) {
                    throw new IllegalArgumentException("AppointmentConfirmed is incomplete.");
                }
            }
            case "AdditionalInformationRejected" -> {
                if (blank(requestEventId) || blank(reason) || blank(message) || expiresAt == null) {
                    throw new IllegalArgumentException("AdditionalInformationRejected is incomplete.");
                }
            }
            case "AppointmentProcessExpired", "AppointmentProcessInvalid" -> {
                if (blank(requestEventId) || blank(reason)) {
                    throw new IllegalArgumentException("The terminal reservation event is incomplete.");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported reservation event type.");
        }
    }

    ReservationEvent toDomain() {
        return new ReservationEvent(eventId, eventType, reservationProcessId, holdId,
                requestEventId, reservationId, professionalId, appointmentDate, startTime,
                status, reason, message, expiresAt, occurredAt, schemaVersion);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
