package ar.edu.um.turnos.appointment.user.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import ar.edu.um.turnos.appointment.user.domain.model.UserRegistration;

/**
 * Body of {@code POST /api/register} (section 3.2 of the statement).
 *
 * <p>The constraints are the HTTP translation of the rules the domain enforces on every
 * registration; they exist to answer a precise {@code fieldErrors} list, not to be the only
 * place where the rules live. {@code imageUrl} is the only optional field.</p>
 *
 * @param login     3 to 50 characters, letters, digits, dot, underscore or hyphen
 * @param password  4 to 100 characters; stored as a hash, never in clear text
 * @param firstName up to 50 characters
 * @param lastName  up to 50 characters
 * @param email     valid address, unique, up to 254 characters
 * @param imageUrl  optional address of the avatar, up to 254 characters
 * @param langKey   2 to 10 characters, language of the interface
 */
public record RegisterRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = UserRegistration.LOGIN_PATTERN,
                message = "must be 3 to 50 characters made of letters, digits, '.', '_' or '-'")
        String login,

        @NotBlank
        @Size(min = 4, max = 100)
        String password,

        @NotBlank
        @Size(max = 50)
        String firstName,

        @NotBlank
        @Size(max = 50)
        String lastName,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Size(max = 254)
        String imageUrl,

        @NotBlank
        @Size(min = 2, max = 10)
        String langKey) {
}
