package ar.edu.um.turnos.appointment.availability.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ar.edu.um.turnos.appointment.availability.domain.model.CatalogProfessional;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklyScheduleQuery;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper.CatalogProfessionalDtoMapper;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper.CatalogWeeklyScheduleDtoMapper;
import ar.edu.um.turnos.appointment.shared.config.CatalogProperties;
import ar.edu.um.turnos.appointment.shared.error.CatalogClientException;
import ar.edu.um.turnos.appointment.shared.security.AuthenticatedUserProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

class CatalogGatewayAdaptersTest {

    private static final String USER_TOKEN = "incoming-user-token-for-propagation-test";

    private MockWebServer server;

    @BeforeEach
    void startServerAndAuthenticateTestUser() throws IOException {
        server = new MockWebServer();
        server.start();
        Jwt jwt = Jwt.withTokenValue(USER_TOKEN)
                .header("alg", "HS256")
                .subject("test.user")
                .claim("publicId", UUID.fromString("ecff720d-7652-4af2-a420-31b8b6a3dbee").toString())
                .issuedAt(java.time.Instant.now().minusSeconds(10))
                .expiresAt(java.time.Instant.now().plusSeconds(300))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void cleanUp() throws IOException {
        SecurityContextHolder.clearContext();
        server.shutdown();
    }

    @Test
    void fetchesProfessionalAndPropagatesUnmodifiedUserJwt() throws Exception {
        server.enqueue(json(200, """
                {"id":15,"categoryId":2,"firstName":"Ana","lastName":"Gomez","enabled":true,
                 "futureField":"ignored"}
                """));

        CatalogProfessional professional = professionalGateway(1000).findProfessional(15);

        assertThat(professional).isEqualTo(new CatalogProfessional(15, 2, "Ana", "Gomez", true));
        RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/internal/professionals/15");
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + USER_TOKEN);
    }

    @Test
    void mapsProfessionalNotFoundToDomainCode() {
        server.enqueue(json(404, "{\"code\":\"PROFESSIONAL_NOT_FOUND\"}"));

        CatalogClientException failure = catchThrowableOfType(
                () -> professionalGateway(1000).findProfessional(404), CatalogClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("PROFESSIONAL_NOT_FOUND");
        assertThat(failure.getHttpStatus()).isEqualTo(404);
    }

    @Test
    void rejectsIncompleteProfessionalResponse() {
        server.enqueue(json(200, """
                {"id":15,"categoryId":2,"lastName":"Gomez","enabled":true}
                """));

        CatalogClientException failure = catchThrowableOfType(
                () -> professionalGateway(1000).findProfessional(15), CatalogClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATALOG_INVALID_RESPONSE");
        assertThat(failure.getHttpStatus()).isEqualTo(502);
    }

    @Test
    void mapsCatalogTimeoutToUnavailable() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\":15,\"categoryId\":2,\"firstName\":\"Ana\",\"lastName\":\"Gomez\",\"enabled\":true}")
                .setBodyDelay(300, TimeUnit.MILLISECONDS));

        CatalogClientException failure = catchThrowableOfType(
                () -> professionalGateway(50).findProfessional(15), CatalogClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATALOG_UNAVAILABLE");
        assertThat(failure.getHttpStatus()).isEqualTo(503);
    }

    @Test
    void fetchesSchedulesForDateAndAcceptsBothTimeFormats() throws Exception {
        server.enqueue(json(200, """
                [
                  {"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00:00","slotDurationMinutes":30},
                  {"dayOfWeek":"MONDAY","startTime":"14:00:00","endTime":"17:00","slotDurationMinutes":30}
                ]
                """));

        List<WeeklySchedule> schedules = scheduleGateway(1000).findWeeklySchedules(
                new WeeklyScheduleQuery(15, LocalDate.parse("2026-09-14")));

        assertThat(schedules).hasSize(2);
        assertThat(schedules.get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedules.get(0).startTime()).isEqualTo(LocalTime.parse("09:00"));
        assertThat(schedules.get(0).endTime()).isEqualTo(LocalTime.parse("12:00"));
        assertThat(schedules.get(0).slotDurationMinutes()).isEqualTo(30);

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath())
                .isEqualTo("/api/internal/professionals/15/weekly-schedules?date=2026-09-14");
        assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + USER_TOKEN);
    }

    @Test
    void fetchesAllEnabledSchedulesWithoutDateFilter() throws Exception {
        server.enqueue(json(200, """
                [{"dayOfWeek":"FRIDAY","startTime":"10:00:00","endTime":"11:00:00","slotDurationMinutes":20}]
                """));

        List<WeeklySchedule> schedules = scheduleGateway(1000).findWeeklySchedules(
                new WeeklyScheduleQuery(21, null));

        assertThat(schedules).hasSize(1);
        assertThat(schedules.get(0).dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(server.takeRequest().getPath())
                .isEqualTo("/api/internal/professionals/21/weekly-schedules");
    }

    @Test
    void acceptsAnEmptyScheduleList() {
        server.enqueue(json(200, "[]"));

        assertThat(scheduleGateway(1000).findWeeklySchedules(new WeeklyScheduleQuery(15, null))).isEmpty();
    }

    @Test
    void mapsScheduleLookupNotFoundToProfessionalNotFound() {
        server.enqueue(json(404, "{\"code\":\"PROFESSIONAL_NOT_FOUND\"}"));

        CatalogClientException failure = catchThrowableOfType(
                () -> scheduleGateway(1000).findWeeklySchedules(new WeeklyScheduleQuery(999, null)),
                CatalogClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("PROFESSIONAL_NOT_FOUND");
        assertThat(failure.getHttpStatus()).isEqualTo(404);
    }

    @Test
    void rejectsMalformedScheduleResponse() {
        server.enqueue(json(200, """
                [{"dayOfWeek":"MONDAY","startTime":"not-a-time","endTime":"12:00","slotDurationMinutes":30}]
                """));

        CatalogClientException failure = catchThrowableOfType(
                () -> scheduleGateway(1000).findWeeklySchedules(new WeeklyScheduleQuery(15, null)),
                CatalogClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATALOG_INVALID_RESPONSE");
        assertThat(failure.getHttpStatus()).isEqualTo(502);
    }

    private CatalogProfessionalGatewayAdapter professionalGateway(long readTimeoutMs) {
        CatalogRestClientSupport support = clientSupport(readTimeoutMs);
        return new CatalogProfessionalGatewayAdapter(
                support, new AuthenticatedUserProvider(), new CatalogProfessionalDtoMapper());
    }

    private CatalogWeeklyScheduleGatewayAdapter scheduleGateway(long readTimeoutMs) {
        CatalogRestClientSupport support = clientSupport(readTimeoutMs);
        return new CatalogWeeklyScheduleGatewayAdapter(
                support, new AuthenticatedUserProvider(), new CatalogWeeklyScheduleDtoMapper());
    }

    private CatalogRestClientSupport clientSupport(long readTimeoutMs) {
        CatalogProperties properties = new CatalogProperties(server.url("/").toString(), 1000, readTimeoutMs);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
        return new CatalogRestClientSupport(restClient, new ObjectMapper().findAndRegisterModules());
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
