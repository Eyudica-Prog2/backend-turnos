package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.Duration;
import java.util.Objects;

public record ReservationRecoveryPolicy(Duration retryBackoff, int batchSize, int pageSize, int maxPages) {

    public ReservationRecoveryPolicy {
        Objects.requireNonNull(retryBackoff, "Retry backoff is required.");
        if (retryBackoff.isNegative() || batchSize < 1 || pageSize < 1 || pageSize > 100 || maxPages < 1) {
            throw new IllegalArgumentException("Reservation recovery limits are invalid.");
        }
    }
}
