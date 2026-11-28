package ar.edu.um.turnos.appointment.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.shared.config.AuthIntrospectionProperties;
import ar.edu.um.turnos.appointment.shared.security.JwtClaims;
import ar.edu.um.turnos.appointment.user.infrastructure.web.controller.TokenIntrospectionController;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenIntrospectionControllerTest {

    private static final String CLIENT_ID = "catalogo";
    private static final String CLIENT_SECRET = "internal-secret";
    private static final String TOKEN = "user-token";
    private static final UUID PUBLIC_ID = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");

    private final JwtDecoder decoder = Mockito.mock(JwtDecoder.class);
    private final TokenIntrospectionController controller = new TokenIntrospectionController(
            decoder, new AuthIntrospectionProperties(CLIENT_ID, CLIENT_SECRET));

    @Test
    void returnsClaimsOnlyForTheConfiguredServiceClient() {
        Instant issuedAt = Instant.parse("2026-11-28T10:00:00Z");
        Jwt jwt = Jwt.withTokenValue(TOKEN)
                .header("alg", "HS256")
                .issuer(JwtClaims.ISSUER)
                .subject("ana")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .claim(JwtClaims.PUBLIC_ID, PUBLIC_ID.toString())
                .claim(JwtClaims.FIRST_NAME, "Ana")
                .claim(JwtClaims.LAST_NAME, "Perez")
                .claim(JwtClaims.AUTHORITIES, List.of("ROLE_USER"))
                .build();
        when(decoder.decode(TOKEN)).thenReturn(jwt);

        var response = controller.introspect(basic(CLIENT_ID, CLIENT_SECRET), TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().active()).isTrue();
        assertThat(response.getBody().sub()).isEqualTo("ana");
        assertThat(response.getBody().iss()).isEqualTo(JwtClaims.ISSUER);
        assertThat(response.getBody().authorities()).containsExactly("ROLE_USER");
    }

    @Test
    void rejectsWrongInternalCredentials() {
        var response = controller.introspect(basic(CLIENT_ID, "wrong-secret"), TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().active()).isFalse();
        Mockito.verifyNoInteractions(decoder);
    }

    @Test
    void returnsInactiveForAnInvalidJwt() {
        when(decoder.decode(TOKEN)).thenThrow(new JwtException("invalid"));

        var response = controller.introspect(basic(CLIENT_ID, CLIENT_SECRET), TOKEN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().active()).isFalse();
    }

    private static String basic(String id, String secret) {
        return "Basic " + Base64.getEncoder().encodeToString(
                (id + ":" + secret).getBytes(StandardCharsets.UTF_8));
    }
}
