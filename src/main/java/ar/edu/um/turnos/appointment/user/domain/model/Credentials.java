package ar.edu.um.turnos.appointment.user.domain.model;

import lombok.Getter;

/**
 * Credentials presented to {@code POST /api/authenticate} (contract: JHipster compatible).
 *
 * <p>Only the edge of the application sees them: the use case verifies them against the
 * stored hash and they are never persisted nor logged. {@code rememberMe} asks for a longer
 * token; it never changes the way the credentials are verified.</p>
 */
@Getter
public class Credentials {

    private final String username;
    private final String password;
    private final boolean rememberMe;

    /**
     * @param username   login presented by the client
     * @param password   clear password presented by the client
     * @param rememberMe when true the issued token lives longer
     */
    public Credentials(String username, String password, boolean rememberMe) {
        this.username = username;
        this.password = password;
        this.rememberMe = rememberMe;
    }
}
