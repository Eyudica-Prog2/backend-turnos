package ar.edu.um.turnos.appointment.user.domain.ports.out;

/**
 * Outbound port: irreversible hashing of the password of an end user.
 *
 * <p>The domain only knows that the password must be stored in a form that cannot be
 * reversed; the algorithm (BCrypt, with its salt and its cost factor) is a decision of the
 * adapter, which keeps this service compatible with the hashes produced by JHipster.</p>
 */
public interface PasswordHasher {

    /**
     * @param rawPassword clear password, only available while the request is being processed
     * @return the hash to persist
     */
    String hash(String rawPassword);

    /**
     * @param rawPassword  clear password presented by the client
     * @param passwordHash hash stored for the account
     * @return true when the password matches the hash
     */
    boolean matches(String rawPassword, String passwordHash);
}
