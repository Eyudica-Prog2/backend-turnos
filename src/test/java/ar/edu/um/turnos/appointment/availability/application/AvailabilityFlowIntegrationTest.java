package ar.edu.um.turnos.appointment.availability.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ar.edu.um.turnos.appointment.availability.application.usecases.GetProfessionalAvailabilityUseCaseImpl;
import ar.edu.um.turnos.appointment.availability.domain.model.AvailabilityQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.ProfessionalAvailability;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.CatedraOccupancyGatewayAdapter;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.CatalogProfessionalGatewayAdapter;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.CatalogRestClientSupport;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.CatalogWeeklyScheduleGatewayAdapter;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper.AppointmentOccupancyDtoMapper;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper.CatalogProfessionalDtoMapper;
import ar.edu.um.turnos.appointment.availability.infrastructure.rest.mapper.CatalogWeeklyScheduleDtoMapper;
import ar.edu.um.turnos.appointment.shared.config.CatedraProperties;
import ar.edu.um.turnos.appointment.shared.config.CatalogProperties;
import ar.edu.um.turnos.appointment.shared.error.CatalogClientException;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.security.AuthenticatedUserProvider;
import ar.edu.um.turnos.appointment.shared.technical.CatedraTechnicalClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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

class AvailabilityFlowIntegrationTest {

    private static final String USER_TOKEN = "authenticated-end-user-jwt-for-flow-test";
    private static final String TECHNICAL_TOKEN = "technical-catedra-jwt-for-flow-test";
    private static final long PROFESSIONAL_ID = 15;
    private static final LocalDate DATE = LocalDate.parse("2026-09-14");

    private MockWebServer catalogServer;
    private MockWebServer catedraServer;
    private GetProfessionalAvailabilityUseCaseImpl useCase;

    @BeforeEach
    void startServersAndComposeRealAdapters() throws IOException {
        catalogServer = new MockWebServer();
        catedraServer = new MockWebServer();
        catalogServer.start();
        catedraServer.start();
        authenticateUserForAdapterCalls();

        CatalogProperties catalogProperties = new CatalogProperties(catalogServer.url("/").toString(), 1000, 1000);
        RestClient catalogRestClient = createRestClient(catalogProperties.baseUrl(), 1000);
        ObjectMapper catalogMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndAddModules()
                .build();
        CatalogRestClientSupport catalogSupport = new CatalogRestClientSupport(catalogRestClient, catalogMapper);
        AuthenticatedUserProvider authenticatedUserProvider = new AuthenticatedUserProvider();
        CatalogProfessionalGatewayAdapter professionalGateway = new CatalogProfessionalGatewayAdapter(
                catalogSupport, authenticatedUserProvider, new CatalogProfessionalDtoMapper());
        CatalogWeeklyScheduleGatewayAdapter scheduleGateway = new CatalogWeeklyScheduleGatewayAdapter(
                catalogSupport, authenticatedUserProvider, new CatalogWeeklyScheduleDtoMapper());

        CatedraProperties catedraProperties = new CatedraProperties(
                new CatedraProperties.Api(catedraServer.url("/").toString(), "", "", TECHNICAL_TOKEN),
                new CatedraProperties.Http(1000, 1000),
                new CatedraProperties.Retry(1, 0, 0));
        ObjectMapper catedraMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndAddModules()
                .build();
        CatedraTechnicalClient technicalClient = new CatedraTechnicalClient(
                catedraProperties, createRestClient(null, 1000), catedraMapper);
        CatedraOccupancyGatewayAdapter occupancyGateway = new CatedraOccupancyGatewayAdapter(
                technicalClient, catedraMapper, new AppointmentOccupancyDtoMapper());

        Clock clock = Clock.fixed(Instant.parse("2026-09-14T08:00:00Z"), ZoneOffset.UTC);
        useCase = new GetProfessionalAvailabilityUseCaseImpl(
                professionalGateway, scheduleGateway, occupancyGateway, clock);
    }

    @AfterEach
    void stopServersAndClearSecurityContext() throws IOException {
        SecurityContextHolder.clearContext();
        catalogServer.shutdown();
        catedraServer.shutdown();
    }

    @Test
    void combinesCatalogSchedulesAndCatedraOccupanciesWithConfirmedTakingPrecedence() throws Exception {
        catalogServer.enqueue(json(200, """
                {"id":15,"categoryId":2,"firstName":"Ana","lastName":"Gomez","enabled":true}
                """));
        catalogServer.enqueue(json(200, """
                [{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00:00","slotDurationMinutes":30}]
                """));
        catedraServer.enqueue(json(200, """
                {
                  "professionalId":15,
                  "from":"2026-09-14",
                  "to":"2026-09-14",
                  "occupancies":[
                    {"date":"2026-09-14","startTime":"09:30:00","status":"CONFIRMED","expiresAt":null},
                    {"date":"2026-09-14","startTime":"10:00:00","status":"HELD","expiresAt":"2026-09-14T08:45:00Z"},
                    {"date":"2026-09-14","startTime":"10:30:00","status":"HELD","expiresAt":"2026-09-14T08:45:00Z"},
                    {"date":"2026-09-14","startTime":"10:30:00","status":"CONFIRMED","expiresAt":null}
                  ]
                }
                """));

        ProfessionalAvailability availability = useCase.getAvailability(
                new AvailabilityQuery(PROFESSIONAL_ID, DATE));

        assertThat(availability.slots())
                .extracting(slot -> slot.startTime().toString())
                .containsExactly("09:00", "11:00", "11:30");

        RecordedRequest professionalRequest = catalogServer.takeRequest();
        RecordedRequest schedulesRequest = catalogServer.takeRequest();
        assertThat(professionalRequest.getPath()).isEqualTo("/api/internal/professionals/15");
        assertThat(schedulesRequest.getPath())
                .isEqualTo("/api/internal/professionals/15/weekly-schedules?date=2026-09-14");
        assertThat(professionalRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + USER_TOKEN);
        assertThat(schedulesRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + USER_TOKEN);

        RecordedRequest occupancyRequest = catedraServer.takeRequest();
        assertThat(occupancyRequest.getPath()).isEqualTo(
                "/api/appointment-occupancies?professionalId=15&from=2026-09-14&to=2026-09-14");
        assertThat(occupancyRequest.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + TECHNICAL_TOKEN);
    }

    @Test
    void stopsAtCatalogAndReportsCatalogUnavailable() {
        catalogServer.enqueue(json(503, "{}"));

        CatalogClientException failure = catchThrowableOfType(
                () -> useCase.getAvailability(new AvailabilityQuery(PROFESSIONAL_ID, DATE)),
                CatalogClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATALOG_UNAVAILABLE");
        assertThat(failure.getHttpStatus()).isEqualTo(503);
        assertThat(catalogServer.getRequestCount()).isEqualTo(1);
        assertThat(catedraServer.getRequestCount()).isZero();
    }

    @Test
    void reportsCatedraUnavailableWhenOccupancyLookupFails() {
        catalogServer.enqueue(json(200, """
                {"id":15,"categoryId":2,"firstName":"Ana","lastName":"Gomez","enabled":true}
                """));
        catalogServer.enqueue(json(200, """
                [{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"10:00","slotDurationMinutes":30}]
                """));
        catedraServer.enqueue(json(503, "{}"));

        CatedraClientException failure = catchThrowableOfType(
                () -> useCase.getAvailability(new AvailabilityQuery(PROFESSIONAL_ID, DATE)),
                CatedraClientException.class);

        assertThat(failure).isNotNull();
        assertThat(failure.getCode()).isEqualTo("CATEDRA_UNAVAILABLE");
        assertThat(failure.getHttpStatus()).isEqualTo(503);
    }

    private static void authenticateUserForAdapterCalls() {
        Jwt jwt = Jwt.withTokenValue(USER_TOKEN)
                .header("alg", "HS256")
                .subject("test.user")
                .claim("publicId", UUID.fromString("ecff720d-7652-4af2-a420-31b8b6a3dbee").toString())
                .issuedAt(Instant.parse("2026-09-14T07:00:00Z"))
                .expiresAt(Instant.parse("2026-09-14T20:00:00Z"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private static RestClient createRestClient(String baseUrl, long readTimeoutMs) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
