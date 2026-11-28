package ar.edu.um.turnos.appointment.user.infrastructure.persistence.adapter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.turnos.appointment.user.application.exception.UserException;
import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.ports.out.UserRepository;
import ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity.AuthorityEntity;
import ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity.UserEntity;
import ar.edu.um.turnos.appointment.user.infrastructure.persistence.mapper.UserMapper;
import ar.edu.um.turnos.appointment.user.infrastructure.persistence.repository.JpaAuthorityRepository;
import ar.edu.um.turnos.appointment.user.infrastructure.persistence.repository.JpaUserRepository;

/**
 * JPA adapter for the {@link UserRepository} port (tables {@code jhi_user},
 * {@code jhi_authority} and {@code jhi_user_authority}).
 *
 * <p>{@link #save} is a multi step operation (the user, then the rows that link it to its
 * roles) and therefore runs in one transaction: an account is either stored with its roles
 * or not at all.</p>
 *
 * <p>The use case already refuses a taken login or e-mail; this adapter translates the
 * violation of those unique constraints when two registrations race each other, so the
 * answer is the same code instead of a {@code 500}. The constraint names come from
 * {@code V3__create_end_users.sql}.</p>
 */
@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private static final String LOGIN_CONSTRAINT = "uq_jhi_user_login";
    private static final String EMAIL_CONSTRAINT = "uq_jhi_user_email";

    private final JpaUserRepository userRepository;
    private final JpaAuthorityRepository authorityRepository;
    private final UserMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsByLogin(String login) {
        return userRepository.existsByLogin(login);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EndUser> findByLogin(String login) {
        return userRepository.findByLogin(login).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public EndUser save(EndUser user) {
        UserEntity entity = mapper.toEntity(user);
        entity.setAuthorities(resolveAuthorities(user.getAuthorities()));
        try {
            // Flushed here on purpose: a violation must surface inside this method to be
            // translated, instead of escaping as a commit error.
            UserEntity stored = userRepository.saveAndFlush(entity);
            return mapper.toDomain(stored);
        } catch (DataIntegrityViolationException conflict) {
            throw translate(conflict);
        }
    }

    /**
     * Resolves the roles the user holds against {@code jhi_authority}. The row is seeded by
     * the migration; it is created only if a future role is ever asked for without its seed,
     * so the foreign key of {@code jhi_user_authority} always finds it.
     *
     * @param names names of the roles, may be empty
     * @return the rows to link to the account
     */
    private Set<AuthorityEntity> resolveAuthorities(List<String> names) {
        Set<AuthorityEntity> rows = new LinkedHashSet<>();
        if (names == null) {
            return rows;
        }
        names.forEach(name -> rows.add(
                authorityRepository.findById(name).orElseGet(() -> new AuthorityEntity(name))));
        return rows;
    }

    private RuntimeException translate(DataIntegrityViolationException conflict) {
        String detail = conflict.getMostSpecificCause().getMessage();
        String message = detail == null ? "" : detail;
        if (message.contains(LOGIN_CONSTRAINT)) {
            return UserException.usernameAlreadyExists();
        }
        if (message.contains(EMAIL_CONSTRAINT)) {
            return UserException.emailAlreadyExists();
        }
        return conflict;
    }
}
