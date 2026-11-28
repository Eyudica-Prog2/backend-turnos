package ar.edu.um.turnos.appointment.user.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity.AuthorityEntity;

/**
 * Spring Data repository of {@code jhi_authority} (lookup table of roles).
 */
public interface JpaAuthorityRepository extends JpaRepository<AuthorityEntity, String> {
}
