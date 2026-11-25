package ar.edu.um.turnos.appointment.reservation.domain.model;

public enum ReservationProcessStatus {
    CREATED,
    HELD,
    WAITING_FOR_PHONE,
    CONFIRMED,
    REJECTED_RETRYABLE,
    EXPIRED,
    INVALID,
    CANCELLED;

    public boolean isFinal() {
        return this == CONFIRMED || this == EXPIRED || this == INVALID || this == CANCELLED;
    }
}
