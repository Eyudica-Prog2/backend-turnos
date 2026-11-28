package ar.edu.um.turnos.appointment.shared.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth.introspection")
public record AuthIntrospectionProperties(@NotBlank String clientId, @NotBlank String clientSecret) {
}
