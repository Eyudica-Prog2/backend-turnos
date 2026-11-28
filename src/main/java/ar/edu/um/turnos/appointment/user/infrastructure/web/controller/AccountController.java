package ar.edu.um.turnos.appointment.user.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.turnos.appointment.user.domain.ports.in.GetAccountUseCase;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.AccountResponse;
import ar.edu.um.turnos.appointment.user.infrastructure.web.mapper.UserDtoMapper;

/**
 * Profile of the authenticated user: {@code GET /api/account} (JHipster compatible).
 *
 * <p>The identity is read from the {@code sub} claim of the token that the security filter
 * already verified (signature, lifetime, issuer), never from a value the client sends in the
 * request: a body or a query parameter cannot move this endpoint to another account.</p>
 *
 * <p>Answers {@code 200} with the profile and no credential, {@code 401} when no token is
 * presented or when the identity no longer resolves to an account.</p>
 */
@RestController
@RequiredArgsConstructor
public class AccountController {

    private final GetAccountUseCase getAccount;
    private final UserDtoMapper mapper;

    /**
     * @param jwt verified token of the request, injected by the security infrastructure
     * @return {@code 200 OK} with the profile of the authenticated user
     */
    @GetMapping("/api/account")
    public ResponseEntity<AccountResponse> account(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(mapper.toResponse(getAccount.getAccount(jwt == null ? null : jwt.getSubject())));
    }
}
