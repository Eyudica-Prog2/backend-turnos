package ar.edu.um.turnos.appointment.user.domain.ports.out;

import java.util.Optional;

import ar.edu.um.turnos.appointment.user.domain.model.EndUser;

/**
 * Outbound port: storage of the end users (tables {@code jhi_user},
 * {@code jhi_authority} and {@code jhi_user_authority}).
 *
 * <p>The port is expressed in domain terms only: it never exposes an entity, a repository
 * nor a Spring type. The password is stored as a hash and this port has no way to read the
 * clear value, because the application never builds one.</p>
 */
public interface UserRepository {

    /**
     * @param login login to look for
     * @return true when an account already uses that login
     */
    boolean existsByLogin(String login);

    /**
     * @param email e-mail to look for
     * @return true when an account already uses that e-mail
     */
    boolean existsByEmail(String email);

    /**
     * @param login login of the account
     * @return the stored account, empty when no account uses that login
     */
    Optional<EndUser> findByLogin(String login);

    /**
     * Stores the account together with its authorities in one transaction.
     *
     * @param user account to store, without id on the way in
     * @return the stored account, with the generated id
     */
    EndUser save(EndUser user);
}
