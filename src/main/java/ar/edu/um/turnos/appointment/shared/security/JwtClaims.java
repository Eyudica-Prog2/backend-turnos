package ar.edu.um.turnos.appointment.shared.security;

/** Claim names issued and validated by backend-turnos. */
public final class JwtClaims {

    public static final String ISSUER = "backend-turnos";
    public static final String PUBLIC_ID = "publicId";
    public static final String AUTHORITIES = "authorities";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";

    private JwtClaims() {
    }
}
