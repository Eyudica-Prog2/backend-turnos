package ar.edu.um.turnos.appointment.user.infrastructure.persistence.mapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity.AuthorityEntity;
import ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity.UserEntity;

/**
 * Maps an end user between the domain and {@code jhi_user}.
 *
 * <p>Two rules worth documenting:</p>
 *
 * <ul>
 *   <li>{@code toEntity} deliberately leaves the authorities empty: they are rows of the
 *       lookup table {@code jhi_authority} and only the adapter can resolve them. The
 *       adapter fills them right after calling this method, before flushing;</li>
 *   <li>the password hash is copied as it is: it is already the stored representation of the
 *       credential, and the clear value is never part of this model.</li>
 * </ul>
 */
@Component
public class UserMapper {

    /**
     * @param entity persisted row, may be null
     * @return the equivalent domain user, or null when the row is null
     */
    public EndUser toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return EndUser.builder()
                .id(entity.getId())
                .publicId(entity.getPublicId())
                .login(entity.getLogin())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .imageUrl(entity.getImageUrl())
                .langKey(entity.getLangKey())
                .activated(entity.isActivated())
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .lastModifiedBy(entity.getLastModifiedBy())
                .authorities(authorityNames(entity.getAuthorities()))
                .build();
    }

    /**
     * @param domain user to persist, may be null
     * @return a row without authorities, ready for the adapter to complete, or null when the
     *         user is null
     */
    public UserEntity toEntity(EndUser domain) {
        if (domain == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setId(domain.getId());
        entity.setPublicId(domain.getPublicId());
        entity.setLogin(domain.getLogin());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setEmail(domain.getEmail());
        entity.setImageUrl(domain.getImageUrl());
        entity.setLangKey(domain.getLangKey());
        entity.setActivated(domain.isActivated());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setModifiedAt(domain.getModifiedAt());
        entity.setLastModifiedBy(domain.getLastModifiedBy());
        return entity;
    }

    private static List<String> authorityNames(java.util.Set<AuthorityEntity> authorities) {
        if (authorities == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>(authorities.size());
        authorities.stream().filter(authority -> authority != null && authority.getName() != null)
                .map(AuthorityEntity::getName)
                .forEach(names::add);
        return List.copyOf(names);
    }
}
