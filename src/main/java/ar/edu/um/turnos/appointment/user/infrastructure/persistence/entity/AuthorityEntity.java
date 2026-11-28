package ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA representation of one row of {@code jhi_authority} (one role).
 *
 * <p>The table is a lookup seeded by the migration {@code V3} with the only role an end user
 * can hold. The adapter resolves the rows it needs before linking them to a user, so the
 * foreign keys of {@code jhi_user_authority} always point to an existing role.</p>
 */
@Entity
@Table(name = "jhi_authority")
@Getter
@Setter
public class AuthorityEntity {

    @Id
    @Column(name = "name", length = 50)
    private String name;

    public AuthorityEntity() {
        // JPA needs a no argument constructor.
    }

    /**
     * @param name name of the role, e.g. {@code ROLE_USER}
     */
    public AuthorityEntity(String name) {
        this.name = name;
    }
}
