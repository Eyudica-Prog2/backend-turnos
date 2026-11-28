package ar.edu.um.turnos.appointment.user.infrastructure.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * Body of {@code GET /api/account}: the profile of the authenticated user.
 *
 * <p>It carries no credential: neither the password nor its hash have a field here, so a
 * mapping mistake cannot leak them. {@code publicId} is the stable identifier other services
 * receive as their own external identifier of the user.</p>
 *
 * @param id          internal id of the account
 * @param publicId    stable identifier of the user across services
 * @param login       login, also the {@code sub} claim of the token
 * @param firstName   first name
 * @param lastName    last name
 * @param email       e-mail, stored in lower case
 * @param imageUrl    optional address of the avatar
 * @param langKey     language of the interface
 * @param activated   always true for an end user registered through this API
 * @param authorities roles of the user, e.g. {@code ["ROLE_USER"]}
 */
public record AccountResponse(Long id,
                              UUID publicId,
                              String login,
                              String firstName,
                              String lastName,
                              String email,
                              String imageUrl,
                              String langKey,
                              boolean activated,
                              List<String> authorities) {
}
