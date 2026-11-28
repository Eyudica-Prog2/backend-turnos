package ar.edu.um.turnos.appointment.user.domain.ports.in;

import ar.edu.um.turnos.appointment.user.domain.model.EndUser;

/**
 * Returns the profile of the authenticated user behind {@code GET /api/account}.
 *
 * <p>The identity comes from the verified token, never from a value sent by the client in
 * the body or in a query parameter.</p>
 */
public interface GetAccountUseCase {

    /**
     * @param login value of the {@code sub} claim of the token that authenticated the request
     * @return the stored account, without any credential
     * @throws RuntimeException with {@code 401 UNAUTHORIZED} when the identity of the token
     *                          does not resolve to an account anymore, so the session must
     *                          be renewed
     */
    EndUser getAccount(String login);
}
