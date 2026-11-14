package ar.edu.um.turnos.appointment.reservation.infrastructure.scheduling;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.reservation.reconciliation")
public record ReservationReconciliationProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("30000") @Min(0) long initialDelayMs,
        @DefaultValue("300000") @Min(1000) long fixedDelayMs,
        @DefaultValue("60000") @Min(0) long retryBackoffMs,
        @DefaultValue("50") @Min(1) @Max(500) int batchSize,
        @DefaultValue("100") @Min(1) @Max(100) int pageSize,
        @DefaultValue("10") @Min(1) @Max(100) int maxPages) {
}
