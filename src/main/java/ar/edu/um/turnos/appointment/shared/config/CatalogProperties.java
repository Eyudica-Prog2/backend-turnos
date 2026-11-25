package ar.edu.um.turnos.appointment.shared.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "catalog.api")
public record CatalogProperties(
        @NotBlank String baseUrl,
        @Min(1) long connectTimeoutMs,
        @Min(1) long readTimeoutMs) {
}
