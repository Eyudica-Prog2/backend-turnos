package ar.edu.um.turnos.appointment.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA representation of one row of {@code jhi_user} (end user of the KMP application).
 *
 * <p>The table keeps the {@code jhi_} prefix and the same columns a JHipster deployment
 * generates, which is what makes both user models observably compatible (statement section
 * 3.2). {@code passwordHash} holds a BCrypt hash: the clear password never reaches this
 * class.</p>
 *
 * <p>The authorities are a many to many over {@code jhi_user_authority}, exactly as the
 * composite primary key of that table describes.</p>
 */
@Entity
@Table(name = "jhi_user")
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable identifier handed out to other services; never changes with the login. */
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "login", nullable = false, length = 50, unique = true)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "image_url", length = 254)
    private String imageUrl;

    @Column(name = "lang_key", length = 10)
    private String langKey;

    /** Active from the registration: there is no e-mail verification step. */
    @Column(name = "activated", nullable = false)
    private boolean activated;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "modified_at", nullable = false)
    private Instant modifiedAt;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "jhi_user_authority",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_name"))
    private Set<AuthorityEntity> authorities = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (modifiedAt == null) {
            modifiedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        modifiedAt = Instant.now();
    }
}
