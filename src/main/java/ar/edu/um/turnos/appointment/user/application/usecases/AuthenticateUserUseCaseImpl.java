package ar.edu.um.turnos.appointment.user.application.usecases;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.user.application.exception.UserException;
import ar.edu.um.turnos.appointment.user.domain.model.Credentials;
import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.model.IssuedToken;
import ar.edu.um.turnos.appointment.user.domain.ports.in.AuthenticateUserUseCase;
import ar.edu.um.turnos.appointment.user.domain.ports.out.AuthTokenIssuer;
import ar.edu.um.turnos.appointment.user.domain.ports.out.PasswordHasher;
import ar.edu.um.turnos.appointment.user.domain.ports.out.UserRepository;

/**
 * Verifies the credentials of an end user and issues the token of the session.
 *
 * <p>Decisions worth documenting:</p>
 *
 * <ul>
 *   <li><strong>no user enumeration</strong>: an unknown login, a wrong password and an
 *       inactive account produce exactly the same {@code 401 UNAUTHORIZED} with the same
 *       detail, and the same amount of work is not required from the caller;</li>
 *   <li><strong>the password is never logged</strong>, neither here nor in the adapter: the
 *       only thing this class does with it is comparing it against the stored hash;</li>
 *   <li>{@code rememberMe} only chooses the lifetime of the token: it never changes how the
 *       credentials are verified, and the reported {@code expires_in} always matches the
 *       token that was issued.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthenticateUserUseCaseImpl.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final AuthTokenIssuer authTokenIssuer;

    @Override
    public IssuedToken authenticate(Credentials credentials) {
        if (credentials == null || isBlank(credentials.getUsername()) || isBlank(credentials.getPassword())) {
            throw UserException.invalidInput("The username and the password are required.");
        }

        EndUser user = userRepository.findByLogin(credentials.getUsername())
                .filter(EndUser::isActivated)
                .filter(EndUser::hasStoredCredential)
                .orElseThrow(UserException::invalidCredentials);

        if (!passwordHasher.matches(credentials.getPassword(), user.getPasswordHash())) {
            log.info("Authentication rejected: login={}", user.getLogin());
            throw UserException.invalidCredentials();
        }

        IssuedToken token = authTokenIssuer.issue(user, credentials.isRememberMe());
        log.info("End user authenticated: login={}, expiresInSeconds={}",
                user.getLogin(), token.getExpiresInSeconds());
        return token;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
