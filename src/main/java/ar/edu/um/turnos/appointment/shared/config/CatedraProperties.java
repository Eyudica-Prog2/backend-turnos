package ar.edu.um.turnos.appointment.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "catedra")
public record CatedraProperties(
        @NotNull @Valid Api api,
        @NotNull @Valid Http http,
        @NotNull @Valid Retry retry) {

    public record Api(
            @NotBlank String baseUrl,
            String login,
            String password,
            String jwt) {

        public boolean hasLoginCredentials() {
            return hasText(login) && hasText(password);
        }

        public boolean hasConfiguredToken() {
            return hasText(jwt);
        }
    }

    public record Http(
            @Min(1) long connectTimeoutMs,
            @Min(1) long readTimeoutMs) {
    }

    public record Retry(
            @Min(1) int maxAttempts,
            @Min(0) long initialBackoffMs,
            @Min(0) long maxBackoffMs) {

        public long backoffFor(int attempt) {
            if (attempt < 1) {
                return 0;
            }
            long multiplier = 1L << Math.min(attempt - 1, 20);
            return Math.min(maxBackoffMs, initialBackoffMs * multiplier);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
