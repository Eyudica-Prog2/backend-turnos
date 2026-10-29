package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import ar.edu.um.turnos.appointment.shared.config.CatedraProperties;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.Duration;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class AppointmentListingGatewayAdapterTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");

    private MockWebServer server;
    private AppointmentListingGatewayAdapter gateway;

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
        gateway = new AppointmentListingGatewayAdapter(client, objectMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void sendsContractFiltersAndMapsPaginationHeaders() throws Exception {
        server.enqueue(json(200, appointmentJson())
                .setHeader("X-Total-Count", "12")
                .setHeader("Link", "<https://catedra.example.test/api/appointments?page=2&size=10"
                        + "&externalPatientId=" + OWNER + ">; rel=\"next\""));

        var page = gateway.list(OWNER, new ReservationQuery("CONFIRMED",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"), 1, 10));
        RecordedRequest request = server.takeRequest();

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getRequestUrl().encodedPath()).isEqualTo("/api/appointments");
        assertThat(request.getRequestUrl().queryParameter("status")).isEqualTo("CONFIRMED");
        assertThat(request.getRequestUrl().queryParameter("from")).isEqualTo("2026-10-01");
        assertThat(request.getRequestUrl().queryParameter("to")).isEqualTo("2026-10-31");
        assertThat(request.getRequestUrl().queryParameter("externalPatientId")).isEqualTo(OWNER.toString());
        assertThat(request.getRequestUrl().queryParameter("page")).isEqualTo("1");
        assertThat(request.getRequestUrl().queryParameter("size")).isEqualTo("10");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer configured-token");
        assertThat(page.totalCount()).isEqualTo(12);
        assertThat(page.link()).isEqualTo("</api/reservations?page=2&size=10>; rel=\"next\"");
        assertThat(page.reservations()).hasSize(1);
        assertThat(page.reservations().getFirst().reservationProcessId()).isEqualTo("process-1");
        assertThat(page.reservations().getFirst().ownerPublicId()).isEqualTo(OWNER);
    }

    @Test
    void rejectsMalformedPaginationHeadersAndMapsUpstreamErrors() {
        server.enqueue(json(200, appointmentJson()).setHeader("X-Total-Count", "not-a-number"));
        assertThatThrownBy(() -> gateway.list(OWNER, new ReservationQuery(null, null, null, 0, 20)))
                .isInstanceOf(CatedraClientException.class)
                .hasMessageContaining("appointments response");

        server.enqueue(json(503, "{}"));
        assertThatThrownBy(() -> gateway.list(OWNER, new ReservationQuery(null, null, null, 0, 20)))
                .isInstanceOf(CatedraClientException.class)
                .satisfies(failure -> assertThat(((CatedraClientException) failure).getHttpStatus()).isEqualTo(503));
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse().setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String appointmentJson() {
        return """
                [{
                  "reservationId": 8251,
                  "reservationProcessId": "process-1",
                  "externalPatientId": "%s",
                  "appointmentDate": "2026-10-26",
                  "startTime": "09:00:00",
                  "endTime": "09:30:00",
                  "professionalId": 15,
                  "professionalFirstName": "Ana",
                  "professionalLastName": "Gomez",
                  "patientFirstName": "Juan",
                  "patientLastName": "Perez",
                  "patientPhone": "+5492615551234",
                  "status": "CONFIRMED",
                  "createdAt": "2026-10-20T10:00:00Z",
                  "confirmedAt": "2026-10-20T10:01:00Z",
                  "cancelledAt": null,
                  "cancellationReason": null
                }]
                """.formatted(OWNER);
    }
}
