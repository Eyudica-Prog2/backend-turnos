package ar.edu.um.turnos.appointment.user.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Token issued after a successful authentication, ready to be handed out to the client.
 *
 * <p>The token itself is a bearer credential: it is returned once, never persisted and never
 * logged. Its content (HS256, issuer {@code backend-turnos} and the claims {@code sub},
 * {@code publicId}, {@code firstName}, {@code lastName} and {@code authorities}) is decided by
 * the adapter that owns the signing key.</p>
 */
@Getter
@Builder
public class IssuedToken {

    /** Value of the token itself, without the {@code Bearer } prefix. */
    private final String value;

    /** Type announced in the response, {@code Bearer}. */
    private final String tokenType;

    /** Number of seconds the token stays valid. */
    private final long expiresInSeconds;

    /**
     * @param value      signed token
     * @param expiresIn  lifetime in seconds
     * @return the token ready to be answered, with the standard type
     */
    public static IssuedToken bearer(String value, long expiresIn) {
        return IssuedToken.builder().value(value).tokenType("Bearer").expiresInSeconds(expiresIn).build();
    }
}
