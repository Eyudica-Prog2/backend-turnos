package ar.edu.um.turnos.appointment.user.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body of a successful {@code POST /api/authenticate}: the token of the session, its type and
 * its lifetime.
 *
 * <p>The field names are the ones of the JHipster contract and are written explicitly, so a
 * change of the global naming strategy cannot break the clients.</p>
 *
 * @param idToken    the JWT itself; presented as {@code Bearer <idToken>} afterwards
 * @param tokenType  always {@code Bearer}
 * @param expiresIn  lifetime of this token in seconds
 */
public record TokenResponse(@JsonProperty("id_token") String idToken,
                            @JsonProperty("token_type") String tokenType,
                            @JsonProperty("expires_in") long expiresIn) {
}
