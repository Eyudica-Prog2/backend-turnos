package ar.edu.um.turnos.appointment.shared.security;

/** Claim names shared with the token issuer in backend-catalogo. */
public final class JwtClaims {

    public static final String ISSUER = "backend-catalogo";
    public static final String PUBLIC_ID = "publicId";
    public static final String AUTHORITIES = "authorities";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";

    private JwtClaims() {
    }
}
