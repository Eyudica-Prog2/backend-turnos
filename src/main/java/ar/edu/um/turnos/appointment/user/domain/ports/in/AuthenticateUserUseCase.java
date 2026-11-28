package ar.edu.um.turnos.appointment.user.domain.ports.in;

import ar.edu.um.turnos.appointment.user.domain.model.Credentials;
import ar.edu.um.turnos.appointment.user.domain.model.IssuedToken;

/**
 * Authenticates an end user and issues the token used during the session.
 *
 * <p>The answer does not reveal which of the two values was wrong: an unknown login and a
 * wrong password produce exactly the same error, so the endpoint cannot be used to discover
 * which accounts exist.</p>
 */
public interface AuthenticateUserUseCase {

    /**
     * Verifies the credentials against the stored hash and issues an HS256 token for the
     * user.
     *
     * @param credentials login, password and whether a longer token is wanted
     * @return the token to hand out to the client
     * @throws RuntimeException with {@code 401 UNAUTHORIZED} when no user matches the login,
     *                          the password does not match the stored hash or the account is
     *                          not active; with {@code 400 VALIDATION_ERROR} when the
     *                          credentials are empty
     */
    IssuedToken authenticate(Credentials credentials);
}
