package ar.edu.um.turnos.appointment.user.infrastructure.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.shared.config.JwtProperties;
import ar.edu.um.turnos.appointment.shared.security.JwtClaims;
import ar.edu.um.turnos.appointment.user.domain.model.EndUser;
import ar.edu.um.turnos.appointment.user.domain.model.IssuedToken;
import ar.edu.um.turnos.appointment.user.domain.ports.out.AuthTokenIssuer;

/**
 * Outbound adapter that issues the end user JWT: HS256, signed with {@code APP_JWT_SECRET} and
 * owned by {@code backend-turnos}. Catálogo validates tokens through the introspection API.
 *
 * <p>Claims of the token (contract of this iteration):</p>
 *
 * <ul>
 *   <li>{@code sub}: login of the user, the identity every other service reads;</li>
 *   <li>{@code publicId}: stable UUID of the account, the value that other services store as
 *       their own external identifier;</li>
 *   <li>{@code firstName} and {@code lastName}: registered profile names needed when a
 *       reservation is initially confirmed with the catedra;</li>
 *   <li>{@code authorities}: roles of the user, read by the security filter of every request
 *       that uses this token;</li>
 *   <li>{@code iss}: {@code backend-turnos}, validated by this service when it consumes its
 *       own tokens;</li>
 *   <li>{@code iat} / {@code exp}: lifetime, {@code rememberMe} selects the longer of the two
 *       configured lifetimes.</li>
 * </ul>
 *
 * <p>The signing key never leaves this adapter: it is read from the environment, used to
 * sign and never written to a log, a response or a message.</p>
 */
@Component
public class JwtTokenIssuer implements AuthTokenIssuer {

    private final JwtProperties properties;

    public JwtTokenIssuer(JwtProperties properties) {
        this.properties = properties;
    }

    @Override
    public IssuedToken issue(EndUser user, boolean rememberMe) {
        if (user == null || user.getLogin() == null) {
            throw new IllegalArgumentException("A token can only be issued for a stored user with a login.");
        }
        long lifetimeSeconds = rememberMe
                ? properties.rememberMeExpirySeconds()
                : properties.expirySeconds();
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + lifetimeSeconds * 1000L);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getLogin())
                .claim(JwtClaims.PUBLIC_ID, publicIdOf(user))
                .claim(JwtClaims.FIRST_NAME, user.getFirstName())
                .claim(JwtClaims.LAST_NAME, user.getLastName())
                .claim(JwtClaims.AUTHORITIES, authoritiesOf(user))
                .issuer(JwtClaims.ISSUER)
                .issueTime(issuedAt)
                .expirationTime(expiresAt)
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(properties.secret().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception signingFailed) {
            // The secret itself is never part of the message: only the fact that signing
            // could not be done.
            throw new IllegalStateException("The session token could not be signed.", signingFailed);
        }
        return IssuedToken.bearer(jwt.serialize(), lifetimeSeconds);
    }

    private static String publicIdOf(EndUser user) {
        if (user.getPublicId() == null) {
            throw new IllegalStateException(
                    "The account " + user.getLogin() + " does not have a publicId yet.");
        }
        return user.getPublicId().toString();
    }

    private static List<String> authoritiesOf(EndUser user) {
        return user.getAuthorities() == null ? List.of() : List.copyOf(user.getAuthorities());
    }
}
