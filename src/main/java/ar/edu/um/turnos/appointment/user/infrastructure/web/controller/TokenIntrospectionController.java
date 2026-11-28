package ar.edu.um.turnos.appointment.user.infrastructure.web.controller;

import ar.edu.um.turnos.appointment.shared.config.AuthIntrospectionProperties;
import ar.edu.um.turnos.appointment.shared.security.JwtClaims;
import ar.edu.um.turnos.appointment.user.infrastructure.web.dto.TokenIntrospectionResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Validates user access tokens for backend-catalogo using the OAuth2 introspection contract. */
@RestController
public class TokenIntrospectionController {

    private static final String BASIC_PREFIX = "Basic ";

    private final JwtDecoder jwtDecoder;
    private final AuthIntrospectionProperties properties;

    public TokenIntrospectionController(JwtDecoder jwtDecoder, AuthIntrospectionProperties properties) {
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
    }

    @PostMapping("/api/internal/auth/introspect")
    public ResponseEntity<TokenIntrospectionResponse> introspect(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("token") String token) {
        if (!validClient(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(TokenIntrospectionResponse.inactive());
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            List<String> authorities = jwt.getClaimAsStringList(JwtClaims.AUTHORITIES);
            return ResponseEntity.ok(new TokenIntrospectionResponse(
                    true,
                    jwt.getSubject(),
                    jwt.getClaimAsString("iss"),
                    jwt.getExpiresAt() == null ? null : jwt.getExpiresAt().getEpochSecond(),
                    jwt.getClaimAsString(JwtClaims.PUBLIC_ID),
                    jwt.getClaimAsString(JwtClaims.FIRST_NAME),
                    jwt.getClaimAsString(JwtClaims.LAST_NAME),
                    authorities == null ? List.of() : authorities));
        } catch (JwtException invalidToken) {
            return ResponseEntity.ok(TokenIntrospectionResponse.inactive());
        }
    }

    private boolean validClient(String authorization) {
        if (authorization == null || !authorization.startsWith(BASIC_PREFIX)) {
            return false;
        }
        try {
            String credentials = new String(Base64.getDecoder().decode(authorization.substring(BASIC_PREFIX.length())),
                    StandardCharsets.UTF_8);
            int separator = credentials.indexOf(':');
            if (separator < 0) {
                return false;
            }
            String clientId = credentials.substring(0, separator);
            String clientSecret = credentials.substring(separator + 1);
            return MessageDigest.isEqual(properties.clientId().getBytes(StandardCharsets.UTF_8),
                    clientId.getBytes(StandardCharsets.UTF_8))
                    && MessageDigest.isEqual(properties.clientSecret().getBytes(StandardCharsets.UTF_8),
                            clientSecret.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException malformedHeader) {
            return false;
        }
    }
}
