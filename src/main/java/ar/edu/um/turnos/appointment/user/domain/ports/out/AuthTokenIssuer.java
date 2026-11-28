package ar.edu.um.turnos.appointment.user.domain.ports.out;

import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.model.IssuedToken;

/**
 * Outbound port: issues the session token of an end user.
 *
 * <p>The application decides <em>who</em> is authenticated; how the proof is built (HS256,
 * the shared secret, the claims and the lifetime) belongs to the adapter, which is also the
 * one that shares the key with {@code backend-turnos}.</p>
 */
public interface AuthTokenIssuer {

    /**
     * @param user       authenticated account
     * @param rememberMe when true the token lives for the longer configured lifetime
     * @return the signed token ready to be answered
     */
    IssuedToken issue(EndUser user, boolean rememberMe);
}
