package ar.edu.um.turnos.appointment.user.infrastructure.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity.UserEntity;

/**
 * Spring Data repository of {@code jhi_user}.
 *
 * <p>Only the queries the ports of the slice need: a mega repository that exposes every
 * table operation would let the application depend on persistence details.</p>
 */
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

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
     * @return the account, empty when there is no such account
     */
    Optional<UserEntity> findByLogin(String login);
}
