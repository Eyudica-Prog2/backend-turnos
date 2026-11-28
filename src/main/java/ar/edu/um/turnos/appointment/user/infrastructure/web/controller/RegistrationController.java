package ar.edu.um.turnos.appointment.user.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.appointment.user.domain.ports.in.RegisterUserUseCase;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.RegisterRequest;
import ar.edu.um.turnos.appointment.user.infrastructure.web.mapper.UserDtoMapper;

/**
 * Registration of a new end user: {@code POST /api/register} (section 3.2 of the statement).
 *
 * <p>Reachable without a token (it is how an anonymous visitor becomes a user, and it is
 * listed as a public endpoint in the contract of iteration 07) but never the other way
 * around: the client cannot choose its id, its {@code publicId}, its role nor its
 * activation, so the answer carries no body and the created account can only be read back
 * through {@code GET /api/account} with the token obtained afterwards.</p>
 *
 * <p>Answers: {@code 201} on success, {@code 400 VALIDATION_ERROR} for an invalid body,
 * {@code 400 USERNAME_ALREADY_EXISTS} / {@code 400 EMAIL_ALREADY_EXISTS} when the login or
 * the e-mail is taken. Any failure reaches the shared error handler.</p>
 */
@RestController
@RequiredArgsConstructor
public class RegistrationController {

    private final RegisterUserUseCase registerUser;
    private final UserDtoMapper mapper;

    /**
     * @param request data of the new account, validated by Jakarta constraints and by the
     *                domain rules of the registration
     * @return {@code 201 Created} without a body
     */
    @PostMapping("/api/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        registerUser.register(mapper.toDomain(request));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
