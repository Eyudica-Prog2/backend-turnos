package ar.edu.um.turnos.appointment.user.infrastructure.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.appointment.user.domain.ports.in.AuthenticateUserUseCase;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.AuthenticateRequest;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.TokenResponse;
import ar.edu.um.turnos.appointment.user.infrastructure.web.mapper.UserDtoMapper;

/**
 * Authentication of an end user: {@code POST /api/authenticate} (JHipster compatible).
 *
 * <p>Reachable without a token, since it is how a token is obtained. Answers {@code 200}
 * with {@code id_token}, {@code token_type} and {@code expires_in}, {@code 400
 * VALIDATION_ERROR} for an empty body and {@code 401 UNAUTHORIZED} when no account matches
 * the credentials. The answer is identical for an unknown login and for a wrong password,
 * and neither the password nor the token are ever written to a log.</p>
 */
@RestController
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticateUserUseCase authenticateUser;
    private final UserDtoMapper mapper;

    /**
     * @param request credentials presented by the client
     * @return {@code 200 OK} with the token of the session
     */
    @PostMapping("/api/authenticate")
    public ResponseEntity<TokenResponse> authenticate(@Valid @RequestBody AuthenticateRequest request) {
        return ResponseEntity.ok(mapper.toResponse(authenticateUser.authenticate(mapper.toDomain(request))));
    }
}
