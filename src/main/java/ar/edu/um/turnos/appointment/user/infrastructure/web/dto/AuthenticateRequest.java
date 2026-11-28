package ar.edu.um.turnos.appointment.user.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /api/authenticate} (JHipster compatible contract).
 *
 * <p>A missing or empty field is answered {@code 400 VALIDATION_ERROR} with its
 * {@code fieldErrors}; credentials that do not match an account are answered
 * {@code 401 UNAUTHORIZED} with the same detail for an unknown login and for a wrong
 * password.</p>
 *
 * <p>{@code rememberMe} is optional and defaults to false: it only selects the longer
 * lifetime of the token, never the way the credentials are verified.</p>
 *
 * @param username   login of the account
 * @param password   password in clear text, used once to verify the stored hash
 * @param rememberMe true to issue a token with the longer configured lifetime
 */
public record AuthenticateRequest(@NotBlank String username,
                                  @NotBlank String password,
                                  Boolean rememberMe) {
}
