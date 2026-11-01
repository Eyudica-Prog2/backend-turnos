package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.turnos.appointment.shared.config.CatedraProperties;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class AppointmentCancellationGatewayAdapterTest {

    private MockWebServer server;
    private AppointmentCancellationGatewayAdapter gateway;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        CatedraProperties properties = new CatedraProperties(
                new CatedraProperties.Api(server.url("/").toString(), null, null, "configured-token"),
                new CatedraProperties.Http(1000, 1000),
                new CatedraProperties.Retry(1, 0, 0));
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(1));
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        CatedraTechnicalClient client = new CatedraTechnicalClient(properties,
                RestClient.builder().requestFactory(requestFactory).build(), objectMapper);
        gateway = new AppointmentCancellationGatewayAdapter(client, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void postsCancellationWithTheContractBodyAndMapsTheResponse() throws Exception {
        server.enqueue(json(200, """
                {"reservationId":8251,"reservationProcessId":"process-1",
                 "status":"CANCELLED","cancelledAt":"2026-10-26T08:00:00Z"}
                """));

        var result = gateway.cancel("process-1", "Cambio de planes");
        RecordedRequest request = server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/appointments/process-1/cancel");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer configured-token");
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"reason\":\"Cambio de planes\"}");
        assertThat(result.reservationId()).isEqualTo(8251L);
        assertThat(result.reservationProcessId()).isEqualTo("process-1");
        assertThat(result.status()).isEqualTo("CANCELLED");
    }

    @Test
    void omitsOptionalBodyAndPreservesCatedraErrorCodes() throws Exception {
        server.enqueue(json(200, """
                {"reservationId":8251,"reservationProcessId":"process-1",
                 "status":"CANCELLED","cancelledAt":"2026-10-26T08:00:00Z"}
                """));
        gateway.cancel("process-1", null);
        assertThat(server.takeRequest().getBody().readUtf8()).isEmpty();

        server.enqueue(json(409, "{\"code\":\"RESERVATION_NOT_CANCELLABLE\"}"));
        assertThatThrownBy(() -> gateway.cancel("process-1", null))
                .isInstanceOf(CatedraClientException.class)
                .satisfies(failure -> {
                    CatedraClientException external = (CatedraClientException) failure;
                    assertThat(external.getCode()).isEqualTo("RESERVATION_NOT_CANCELLABLE");
                    assertThat(external.getHttpStatus()).isEqualTo(409);
                });
    }

    @Test
    void rejectsMalformedOrMismatchedSuccessResponses() {
        server.enqueue(json(200, "{\"reservationProcessId\":\"other\",\"status\":\"CANCELLED\"}"));
        assertThatThrownBy(() -> gateway.cancel("process-1", null))
                .isInstanceOf(CatedraClientException.class)
                .satisfies(failure -> assertThat(((CatedraClientException) failure).getCode())
                        .isEqualTo("CATEDRA_INVALID_RESPONSE"));
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse().setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
