package ar.edu.um.turnos.appointment.user.domain.ports.in;

import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.model.UserRegistration;

/**
 * Registers a new end user of the KMP application (section 3.2 of the statement).
 *
 * <p>The user is active from the moment of the registration: there is no e-mail
 * verification step and no other state the client can choose. The password is hashed by the
 * application before it reaches the repository.</p>
 */
public interface RegisterUserUseCase {

    /**
     * Validates the registration, refuses a login or an e-mail that is already taken, hashes
     * the password and stores the new user with the role {@code ROLE_USER}.
     *
     * @param registration data of the registration
     * @return the stored user, including its generated {@code publicId} and id
     * @throws RuntimeException with {@code 400 VALIDATION_ERROR} when the data does not
     *                          describe a valid registration, with
     *                          {@code 400 USERNAME_ALREADY_EXISTS} or
     *                          {@code 400 EMAIL_ALREADY_EXISTS} when it collides with an
     *                          existing account
     */
    EndUser register(UserRegistration registration);
}
