package ar.edu.um.turnos.appointment.catedra.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ar.edu.um.turnos.appointment.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.appointment.shared.config.CatedraProperties;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class CatedraIntegrationGatewayAdapterTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    void authenticatesAndFetchesCurrentIntegration() throws Exception {
        server.enqueue(json(200, authenticationResponse()));
        server.enqueue(json(200, integrationResponse()));

        CatedraIntegration integration = gateway(properties(2, 1000, "tech-login", "tech-password", ""))
                .fetchCurrentIntegration();

        assertThat(integration.groupId()).isEqualTo("proyecto-ejemplo");
        assertThat(integration.provisioningStatus()).isEqualTo("PROVISIONED");
        assertThat(integration.redisPassword()).isEqualTo("redis-secret-for-tests");
        assertThat(integration.toString()).doesNotContain("redis-secret-for-tests");
        assertThat(server.getRequestCount()).isEqualTo(2);

        RecordedRequest authentication = server.takeRequest();
        assertThat(authentication.getMethod()).isEqualTo("POST");
        assertThat(authentication.getPath()).isEqualTo("/api/authenticate");
        assertThat(authentication.getBody().readUtf8())
                .contains("\"username\":\"tech-login\"")
                .contains("\"password\":\"tech-password\"")
                .contains("\"rememberMe\":true");

        RecordedRequest currentIntegration = server.takeRequest();
        assertThat(currentIntegration.getMethod()).isEqualTo("GET");
        assertThat(currentIntegration.getPath()).isEqualTo("/api/student/integration");
        assertThat(currentIntegration.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer technical-token-1");
    }

    @Test
    void reusesTechnicalTokenForSubsequentIntegrationReads() {
        server.enqueue(json(200, authenticationResponse()));
        server.enqueue(json(200, integrationResponse()));
        server.enqueue(json(200, integrationResponse()));

        CatedraIntegrationGatewayAdapter client = gateway(properties(2, 1000, "tech-login", "tech-password", ""));
        client.fetchCurrentIntegration();
        client.fetchCurrentIntegration();

        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void reauthenticatesOnceWhenCatedraRejectsCachedToken() throws Exception {
        server.enqueue(json(200, authenticationResponse()));
        server.enqueue(json(401, "{\"code\":\"UNAUTHORIZED\"}"));
        server.enqueue(json(200, authenticationResponse("technical-token-2")));
        server.enqueue(json(200, integrationResponse()));

        CatedraIntegration result = gateway(properties(2, 1000, "tech-login", "tech-password", ""))
                .fetchCurrentIntegration();

        assertThat(result.groupId()).isEqualTo("proyecto-ejemplo");
        assertThat(server.getRequestCount()).isEqualTo(4);
        server.takeRequest();
        server.takeRequest();
        server.takeRequest();
        RecordedRequest retriedRead = server.takeRequest();
        assertThat(retriedRead.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer technical-token-2");
    }

    @Test
    void rejectsCredentialsWithoutRetryingAnAuthentication401() {
        server.enqueue(json(401, "{\"detail\":\"Credentials rejected\"}"));

        CatedraClientException failure = catchThrowableOfType(
                () -> gateway(properties(3, 1000, "tech-login", "wrong-password", ""))
                        .fetchCurrentIntegration(),
                CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATEDRA_AUTH_FAILED");
        assertThat(failure.getHttpStatus()).isEqualTo(503);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void preservesCatedraProblemCodeAndDoesNotUseDetailForErrorTranslation() {
        server.enqueue(json(200, authenticationResponse()));
        server.enqueue(json(400, """
                {"status":400,"code":"INVALID_DATE_RANGE","detail":"untrusted detail text"}
                """));

        CatedraClientException failure = catchThrowableOfType(
                () -> gateway(properties(1, 1000, "tech-login", "tech-password", ""))
                        .fetchCurrentIntegration(),
                CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("INVALID_DATE_RANGE");
        assertThat(failure.getHttpStatus()).isEqualTo(400);
        assertThat(failure.getMessage()).doesNotContain("untrusted detail text");
    }

    @Test
    void failsFastWhenTechnicalCredentialsAndTokenAreMissing() {
        CatedraClientException failure = catchThrowableOfType(
                () -> gateway(properties(2, 1000, "", "", "")).fetchCurrentIntegration(),
                CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATEDRA_NOT_CONFIGURED");
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void retriesTimeoutsOnlyUpToConfiguredMaximum() {
        server.enqueue(json(200, authenticationResponse()));
        server.enqueue(delayedJson(200, integrationResponse(), 400));
        server.enqueue(delayedJson(200, integrationResponse(), 400));

        CatedraClientException failure = catchThrowableOfType(
                () -> gateway(properties(2, 50, "tech-login", "tech-password", ""))
                        .fetchCurrentIntegration(),
                CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATEDRA_UNAVAILABLE");
        assertThat(failure.getHttpStatus()).isEqualTo(503);
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void neverRetriesANonIdempotentPostAfterAnAmbiguousTimeout() {
        server.enqueue(delayedJson(201, "{\"accepted\":true}", 300));
        CatedraProperties properties = properties(3, 50, "", "", "configured-token-for-tests");

        CatedraClientException failure = catchThrowableOfType(
                () -> technicalClient(properties).post("/api/appointment-holds", "request-body"),
                CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATEDRA_UNAVAILABLE");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void canUseAnExternallyConfiguredTechnicalTokenWithoutLogin() throws Exception {
        server.enqueue(json(200, integrationResponse()));

        gateway(properties(2, 1000, "", "", "configured-token-for-tests"))
                .fetchCurrentIntegration();

        RecordedRequest integration = server.takeRequest();
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(integration.getPath()).isEqualTo("/api/student/integration");
        assertThat(integration.getHeader(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer configured-token-for-tests");
    }

    private CatedraIntegrationGatewayAdapter gateway(CatedraProperties properties) {
        CatedraTechnicalClient technicalClient = technicalClient(properties);
        ObjectMapper objectMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        return new CatedraIntegrationGatewayAdapter(
                technicalClient,
                objectMapper,
                new ar.edu.um.turnos.appointment.catedra.infrastructure.rest.mapper
                        .CatedraIntegrationResponseMapper());
    }

    private CatedraTechnicalClient technicalClient(CatedraProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.http().connectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.http().readTimeoutMs()));
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        ObjectMapper objectMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();

        return new CatedraTechnicalClient(properties, restClient, objectMapper);
    }

    private CatedraProperties properties(int maxAttempts, long readTimeoutMs,
                                         String login, String password, String jwt) {
        return new CatedraProperties(
                new CatedraProperties.Api(server.url("/").toString(), login, password, jwt),
                new CatedraProperties.Http(1000, readTimeoutMs),
                new CatedraProperties.Retry(maxAttempts, 0, 0));
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockResponse delayedJson(int status, String body, long delayMs) {
        return json(status, body).setBodyDelay(delayMs, TimeUnit.MILLISECONDS);
    }

    private static String authenticationResponse() {
        return authenticationResponse("technical-token-1");
    }

    private static String authenticationResponse(String token) {
        return "{\"id_token\":\"" + token + "\",\"integration\":" + integrationResponse() + "}";
    }

    private static String integrationResponse() {
        return """
                {
                  "groupId": "proyecto-ejemplo",
                  "redisHost": "redis.example.test",
                  "redisPort": 6379,
                  "redisUsername": "grp_proyecto-ejemplo",
                  "redisPassword": "redis-secret-for-tests",
                  "redisReadNamespace": "catedra:sync:*",
                  "redisWriteNamespace": "alumnos:proyecto-ejemplo:*",
                  "kafkaBootstrapServers": "kafka.example.test:9092",
                  "kafkaConsumerGroupId": "alumnos-proyecto-ejemplo",
                  "kafkaCatalogTopic": "catedra.catalog.proyecto-ejemplo",
                  "kafkaAppointmentActionsTopic": "alumnos.turnos.acciones.proyecto-ejemplo",
                  "kafkaAppointmentPhoneTopic": "catedra.turnos.telefono.proyecto-ejemplo",
                  "provisioningStatus": "PROVISIONED",
                  "futureField": "ignored"
                }
                """;
    }
}
