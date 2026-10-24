package ar.edu.um.turnos.appointment.availability.infrastructure.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentSlot;
import ar.edu.um.turnos.appointment.availability.domain.model.AvailabilityQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.ProfessionalAvailability;
import ar.edu.um.turnos.appointment.availability.domain.ports.in.GetProfessionalAvailabilityUseCase;
import ar.edu.um.turnos.appointment.availability.infrastructure.web.mapper.AvailabilityDtoMapper;
import ar.edu.um.turnos.appointment.shared.error.CatalogClientException;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.error.ApiExceptionHandler;
import ar.edu.um.turnos.appointment.shared.security.JwtClaims;
import ar.edu.um.turnos.appointment.shared.security.JwtSecurityConfiguration;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = AvailabilityController.class,
        properties = "app.jwt.secret=test-only-shared-secret-with-32-bytes-minimum")
@Import({JwtSecurityConfiguration.class, AvailabilityDtoMapper.class, ApiExceptionHandler.class})
class AvailabilityControllerTest {

    private static final String JWT_SECRET = "test-only-shared-secret-with-32-bytes-minimum";
    private static final UUID PUBLIC_ID = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetProfessionalAvailabilityUseCase getAvailability;

    @Test
    void returnsAvailableSlotsForAuthenticatedUser() throws Exception {
        LocalDate date = LocalDate.parse("2026-09-14");
        AvailabilityQuery query = new AvailabilityQuery(15, date);
        when(getAvailability.getAvailability(query)).thenReturn(new ProfessionalAvailability(
                15,
                date,
                List.of(new AppointmentSlot(date, LocalTime.parse("09:00"), LocalTime.parse("09:30")))));

        mockMvc.perform(get("/api/professionals/15/availability")
                        .queryParam("date", "2026-09-14")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalId").value(15))
                .andExpect(jsonPath("$.date").value("2026-09-14"))
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(1))
                .andExpect(jsonPath("$.slots[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("09:30:00"));
    }

    @Test
    void rejectsTheEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/professionals/15/availability")
                        .queryParam("date", "2026-09-14"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidDateWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/professionals/15/availability")
                        .queryParam("date", "not-a-date")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        org.mockito.Mockito.verifyNoInteractions(getAvailability);
    }

    @Test
    void translatesCatalogOutageToServiceUnavailable() throws Exception {
        LocalDate date = LocalDate.parse("2026-09-14");
        when(getAvailability.getAvailability(new AvailabilityQuery(15, date)))
                .thenThrow(new CatalogClientException(
                        "CATALOG_UNAVAILABLE", 503, "The catalog service is not available."));

        mockMvc.perform(get("/api/professionals/15/availability")
                        .queryParam("date", "2026-09-14")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CATALOG_UNAVAILABLE"));
    }

    @Test
    void mapsUnknownProfessionalToNotFound() throws Exception {
        LocalDate date = LocalDate.parse("2026-09-14");
        when(getAvailability.getAvailability(new AvailabilityQuery(404, date)))
                .thenThrow(new CatalogClientException(
                        "PROFESSIONAL_NOT_FOUND", 404, "The professional was not found in the catalog."));

        mockMvc.perform(get("/api/professionals/404/availability")
                        .queryParam("date", "2026-09-14")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROFESSIONAL_NOT_FOUND"));
    }

    @Test
    void preservesCatedraErrorCodeInProblemResponse() throws Exception {
        LocalDate date = LocalDate.parse("2026-09-14");
        when(getAvailability.getAvailability(new AvailabilityQuery(15, date)))
                .thenThrow(new CatedraClientException(
                        "INVALID_DATE_RANGE", 400, "The catedra rejected the date range."));

        mockMvc.perform(get("/api/professionals/15/availability")
                        .queryParam("date", "2026-09-14")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    private static String userToken() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(JwtClaims.ISSUER)
                .subject("test.user")
                .issueTime(Date.from(now.minusSeconds(10)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim(JwtClaims.PUBLIC_ID, PUBLIC_ID.toString())
                .claim(JwtClaims.AUTHORITIES, List.of("ROLE_USER"))
                .build();
        SignedJWT signedJwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));
        return signedJwt.serialize();
    }
}
