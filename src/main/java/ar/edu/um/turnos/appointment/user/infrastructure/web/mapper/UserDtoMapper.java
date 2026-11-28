package ar.edu.um.turnos.appointment.user.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.user.domain.model.Credentials;
import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.model.IssuedToken;
import ar.edu.um.turnos.appointment.user.domain.model.UserRegistration;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.AccountResponse;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.AuthenticateRequest;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.RegisterRequest;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.TokenResponse;

/**
 * Maps the HTTP bodies of the {@code user} slice into domain values and back.
 *
 * <p>It is a pure translation: no rule is decided here. The rules of a valid registration
 * live in {@link UserRegistration} (checked by the use case) and their Jakarta counterparts
 * live in the request record, so the HTTP layer never becomes the only place where a rule
 * exists.</p>
 */
@Component
public class UserDtoMapper {

    /**
     * @param request body of {@code POST /api/register}, may be null
     * @return the equivalent registration, or null when the body is null (the use case then
     *         answers {@code 400 VALIDATION_ERROR})
     */
    public UserRegistration toDomain(RegisterRequest request) {
        if (request == null) {
            return null;
        }
        return UserRegistration.builder()
                .login(request.login())
                .password(request.password())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .imageUrl(request.imageUrl())
                .langKey(request.langKey())
                .build();
    }

    /**
     * @param request body of {@code POST /api/authenticate}, may be null
     * @return the equivalent credentials; a missing {@code rememberMe} means false
     */
    public Credentials toDomain(AuthenticateRequest request) {
        if (request == null) {
            return null;
        }
        return new Credentials(request.username(), request.password(),
                Boolean.TRUE.equals(request.rememberMe()));
    }

    /**
     * @param token token just issued by the use case, may be null
     * @return the body of a successful authentication, or null when the token is null
     */
    public TokenResponse toResponse(IssuedToken token) {
        if (token == null) {
            return null;
        }
        return new TokenResponse(token.getValue(), token.getTokenType(), token.getExpiresInSeconds());
    }

    /**
     * @param user account of the authenticated user, may be null
     * @return the body of {@code GET /api/account}, or null when the user is null
     */
    public AccountResponse toResponse(EndUser user) {
        if (user == null) {
            return null;
        }
        return new AccountResponse(
                user.getId(),
                user.getPublicId(),
                user.getLogin(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getImageUrl(),
                user.getLangKey(),
                user.isActivated(),
                user.getAuthorities());
    }
}
