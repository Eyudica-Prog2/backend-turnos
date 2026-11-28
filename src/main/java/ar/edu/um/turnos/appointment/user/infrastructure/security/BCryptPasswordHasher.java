package ar.edu.um.turnos.appointment.user.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.user.domain.ports.out.PasswordHasher;

/**
 * Outbound adapter that hashes the password of an end user with BCrypt.
 *
 * <p>Decisions worth documenting:</p>
 *
 * <ul>
 *   <li><strong>BCrypt</strong> is used because it is the algorithm of JHipster, so a hash
 *       produced by this service and one produced by a JHipster deployment have the same
 *       shape (60 characters, self describing cost factor);</li>
 *   <li>every hash carries its own salt and the cost factor, so two users with the same
 *       password do not produce the same value and the cost can be raised later without a
 *       migration;</li>
 *   <li>{@code matches} is deliberately constant in its contract: it answers yes or no and
 *       never tells the caller <em>why</em>, which is what keeps an authentication error
 *       indistinguishable from any other.</li>
 * </ul>
 *
 * <p>The clear password only exists as the argument of these two methods: this adapter never
 * stores it, never logs it and never returns it.</p>
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            // Refused here as well: hashing an empty value would store a "valid" hash of
            // nothing and silently weaken the rule that the password is required.
            throw new IllegalArgumentException("A password is required to build its hash.");
        }
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        return encoder.matches(rawPassword, passwordHash);
    }
}
