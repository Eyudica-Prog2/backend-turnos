package ar.edu.um.turnos.appointment.user.application.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.user.application.exception.UserException;
import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.ports.in.GetAccountUseCase;
import ar.edu.um.turnos.appointment.user.domain.ports.out.UserRepository;

/**
 * Returns the profile behind {@code GET /api/account}.
 *
 * <p>The login comes from the {@code sub} claim of a token whose signature, lifetime and
 * authorities were already verified by the security filter, so the client never chooses the
 * identity that is read. The answer contains no credential: the password hash stays in the
 * database and the response is built by the DTO mapper, which has no field for it.</p>
 */
@Component
@RequiredArgsConstructor
public class GetAccountUseCaseImpl implements GetAccountUseCase {

    private final UserRepository userRepository;

    @Override
    public EndUser getAccount(String login) {
        if (login == null || login.isBlank()) {
            throw UserException.unknownIdentity();
        }
        return userRepository.findByLogin(login).orElseThrow(UserException::unknownIdentity);
    }
}
