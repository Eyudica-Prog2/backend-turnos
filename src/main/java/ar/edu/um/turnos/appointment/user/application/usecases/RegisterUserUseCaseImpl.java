package ar.edu.um.turnos.appointment.user.application.usecases;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.user.application.exception.UserException;
import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.model.UserRegistration;
import ar.edu.um.turnos.appointment.user.domain.ports.in.RegisterUserUseCase;
import ar.edu.um.turnos.appointment.user.domain.ports.out.PasswordHasher;
import ar.edu.um.turnos.appointment.user.domain.ports.out.UserRepository;

/**
 * Registers a new end user (section 3.2 of the statement).
 *
 * <p>Order of the steps, each one for a reason:</p>
 *
 * <ol>
 *   <li>the rules of a valid registration are checked by the domain, so an invalid data set
 *       never reaches the password hashing nor the database;</li>
 *   <li>the uniqueness of the login and of the e-mail is checked before hashing: the answer
 *       is cheaper and the error is specific ({@code USERNAME_ALREADY_EXISTS} vs
 *       {@code EMAIL_ALREADY_EXISTS}); the constraints of the table are still the final
 *       authority and a concurrent registration is translated back to the same codes by the
 *       adapter;</li>
 *   <li>the password is hashed exactly once and the clear value only lives in the local
 *       variable of this method: it is never logged, never stored and never returned;</li>
 *   <li>the account is stored active, with the role {@code ROLE_USER} and with a generated
 *       {@code publicId}: the client cannot choose any of them.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegisterUserUseCaseImpl.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Override
    public EndUser register(UserRegistration registration) {
        if (registration == null) {
            throw UserException.invalidInput("A registration body is required.");
        }
        validate(registration);

        String email = registration.getNormalizedEmail();
        if (userRepository.existsByLogin(registration.getLogin())) {
            throw UserException.usernameAlreadyExists();
        }
        if (userRepository.existsByEmail(email)) {
            throw UserException.emailAlreadyExists();
        }

        String passwordHash = passwordHasher.hash(registration.getPassword());
        EndUser candidate = EndUser.registered(registration, UUID.randomUUID(), passwordHash, Instant.now());
        EndUser stored = userRepository.save(candidate);
        // Only the login is logged: neither the password nor the hash ever reach a log.
        log.info("End user registered: login={}, publicId={}", stored.getLogin(), stored.getPublicId());
        return stored;
    }

    private static void validate(UserRegistration registration) {
        try {
            registration.validate();
        } catch (IllegalArgumentException rejected) {
            throw UserException.invalidInput(rejected.getMessage());
        }
    }
}
