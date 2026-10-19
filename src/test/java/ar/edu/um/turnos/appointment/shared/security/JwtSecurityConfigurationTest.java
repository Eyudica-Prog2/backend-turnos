package ar.edu.um.turnos.appointment.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(
        controllers = JwtSecurityConfigurationTest.SecurityProbeController.class,
        properties = "app.jwt.secret=test-only-shared-secret-with-32-bytes-minimum")
@Import({
        JwtSecurityConfiguration.class,
        AuthenticatedUserProvider.class,
        JwtSecurityConfigurationTest.SecurityProbeController.class
})
class JwtSecurityConfigurationTest {

    private static final String TEST_SECRET = "test-only-shared-secret-with-32-bytes-minimum";
    private static final UUID PUBLIC_ID = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/test/security/identity"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsCatalogTokenAndExposesAuthenticatedIdentity() throws Exception {
        mockMvc.perform(get("/test/security/identity")
                        .header("Authorization", "Bearer " + token(TEST_SECRET, "backend-catalogo", true,
                                List.of("ROLE_USER"), PUBLIC_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("test.user"))
                .andExpect(jsonPath("$.publicId").value(PUBLIC_ID.toString()))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"));
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() throws Exception {
        mockMvc.perform(get("/test/security/identity")
                        .header("Authorization", "Bearer " + token("another-test-secret-with-32-bytes-minimum",
                                "backend-catalogo", true, List.of("ROLE_USER"), PUBLIC_ID.toString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenFromAnotherIssuer() throws Exception {
        mockMvc.perform(get("/test/security/identity")
                        .header("Authorization", "Bearer " + token(TEST_SECRET, "some-other-issuer", true,
                                List.of("ROLE_USER"), PUBLIC_ID.toString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        mockMvc.perform(get("/test/security/identity")
                        .header("Authorization", "Bearer " + token(TEST_SECRET, "backend-catalogo", false,
                                List.of("ROLE_USER"), PUBLIC_ID.toString())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAuthenticatedTokenWithoutUserAuthority() throws Exception {
        mockMvc.perform(get("/test/security/identity")
                        .header("Authorization", "Bearer " + token(TEST_SECRET, "backend-catalogo", true,
                                List.of(), PUBLIC_ID.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsTokenWithoutValidPublicId() throws Exception {
        mockMvc.perform(get("/test/security/identity")
                        .header("Authorization", "Bearer " + token(TEST_SECRET, "backend-catalogo", true,
                                List.of("ROLE_USER"), "not-a-uuid")))
                .andExpect(status().isUnauthorized());
    }

    private static String token(String secret, String issuer, boolean unexpired,
                                List<String> authorities, String publicId) throws Exception {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now.minusSeconds(30));
        Date expiresAt = Date.from(unexpired ? now.plusSeconds(300) : now.minusSeconds(120));
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("test.user")
                .issueTime(issuedAt)
                .expirationTime(expiresAt)
                .claim(JwtClaims.PUBLIC_ID, publicId)
                .claim(JwtClaims.AUTHORITIES, authorities)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return signedJWT.serialize();
    }

    @RestController
    static class SecurityProbeController {

        private final AuthenticatedUserProvider authenticatedUserProvider;

        SecurityProbeController(AuthenticatedUserProvider authenticatedUserProvider) {
            this.authenticatedUserProvider = authenticatedUserProvider;
        }

        @GetMapping(value = "/test/security/identity", produces = MediaType.APPLICATION_JSON_VALUE)
        AuthenticatedUser identity() {
            return authenticatedUserProvider.currentUser();
        }
    }
}
