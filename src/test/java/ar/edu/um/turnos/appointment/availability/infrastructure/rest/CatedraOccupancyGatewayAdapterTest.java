package ar.edu.um.turnos.appointment.availability.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyStatus;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.dto.AppointmentOccupanciesResponse;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper.AppointmentOccupancyDtoMapper;
import ar.edu.um.turnos.appointment.catedra.infrastructure.rest.CatedraClientException;
import ar.edu.um.turnos.appointment.catedra.infrastructure.rest.CatedraTechnicalClient;
import ar.edu.um.turnos.appointment.shared.config.CatedraProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class CatedraOccupancyGatewayAdapterTest {

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
    void requestsOccupanciesAndMapsConfirmedAndHeldEntries() throws Exception {
        server.enqueue(json(200, occupanciesJson()));
        OccupancyQuery query = query();

        List<AppointmentOccupancy> result = gateway().findOccupancies(query);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).status()).isEqualTo(OccupancyStatus.CONFIRMED);
        assertThat(result.get(0).date()).isEqualTo(LocalDate.parse("2026-09-14"));
        assertThat(result.get(0).startTime()).isEqualTo(LocalTime.parse("09:00:00"));
        assertThat(result.get(0).expiresAt()).isNull();
        assertThat(result.get(1).status()).isEqualTo(OccupancyStatus.HELD);
        assertThat(result.get(1).startTime()).isEqualTo(LocalTime.parse("09:30:00"));
        assertThat(result.get(1).expiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo(
                "/api/appointment-occupancies?professionalId=15&from=2026-09-14&to=2026-09-14");
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer technical-token-for-test");
    }

    @Test
    void acceptsAnEmptyOccupancyList() {
        server.enqueue(json(200, """
                {"professionalId":15,"from":"2026-09-14","to":"2026-09-14","occupancies":[]}
                """));

        assertThat(gateway().findOccupancies(query())).isEmpty();
    }

    @Test
    void rejectsAnInvalidDateRangeBeforeMakingHttpCall() {
        IllegalArgumentException failure = catchThrowableOfType(
                () -> new OccupancyQuery(15, LocalDate.parse("2026-09-15"), LocalDate.parse("2026-09-14")),
                IllegalArgumentException.class);

        assertThat(failure).hasMessage("From date cannot be after to date.");
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void rejectsInvalidProfessionalIdBeforeMakingHttpCall() {
        assertThat(catchThrowableOfType(
                () -> new OccupancyQuery(0, LocalDate.parse("2026-09-14"), LocalDate.parse("2026-09-14")),
                IllegalArgumentException.class))
                .hasMessage("Professional ID must be positive.");
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void reportsExternalHttpErrorsWithoutLeakingResponseBody() {
        server.enqueue(json(400, """
                {"status":400,"code":"INVALID_DATE_RANGE","detail":"private upstream detail"}
                """));

        CatedraClientException failure = catchThrowableOfType(
                () -> gateway().findOccupancies(query()), CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getHttpStatus()).isEqualTo(400);
        assertThat(failure.getCode()).isEqualTo("CATEDRA_ERROR");
        assertThat(failure.getMessage()).doesNotContain("private upstream detail");
    }

    @Test
    void rejectsMalformedOccupancyPayload() {
        server.enqueue(json(200, """
                {"professionalId":15,"from":"2026-09-14","to":"2026-09-14",
                 "occupancies":[{"date":"2026-09-14","startTime":"09:00","status":"UNKNOWN","expiresAt":null}]}
                """));

        CatedraClientException failure = catchThrowableOfType(
                () -> gateway().findOccupancies(query()), CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATEDRA_INVALID_RESPONSE");
        assertThat(failure.getHttpStatus()).isEqualTo(503);
    }

    @Test
    void rejectsAResponseForAnotherProfessionalOrDateRange() {
        server.enqueue(json(200, """
                {"professionalId":99,"from":"2026-09-14","to":"2026-09-14","occupancies":[]}
                """));

        CatedraClientException failure = catchThrowableOfType(
                () -> gateway().findOccupancies(query()), CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATEDRA_INVALID_RESPONSE");
    }

    private CatedraOccupancyGatewayAdapter gateway() {
        CatedraProperties properties = new CatedraProperties(
                new CatedraProperties.Api(server.url("/").toString(), "", "", "technical-token-for-test"),
                new CatedraProperties.Http(1000, 1000),
                new CatedraProperties.Retry(1, 0, 0));
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(1000));
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        CatedraTechnicalClient technicalClient = new CatedraTechnicalClient(properties, restClient, objectMapper);
        return new CatedraOccupancyGatewayAdapter(
                technicalClient, objectMapper, new AppointmentOccupancyDtoMapper());
    }

    private static OccupancyQuery query() {
        return new OccupancyQuery(15, LocalDate.parse("2026-09-14"), LocalDate.parse("2026-09-14"));
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String occupanciesJson() {
        return """
                {
                  "professionalId": 15,
                  "from": "2026-09-14",
                  "to": "2026-09-14",
                  "occupancies": [
                    {"date":"2026-09-14","startTime":"09:00:00","status":"CONFIRMED","expiresAt":null},
                    {"date":"2026-09-14","startTime":"09:30","status":"HELD","expiresAt":"2026-09-10T14:44:50Z"}
                  ],
                  "futureField": "ignored"
                }
                """;
    }
}
