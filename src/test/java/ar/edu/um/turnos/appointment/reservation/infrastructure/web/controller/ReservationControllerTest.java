package ar.edu.um.turnos.appointment.reservation.infrastructure.web.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ConfirmReservationUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.CreateReservationUseCase;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import ar.edu.um.turnos.appointment.shared.error.ApiExceptionHandler;
import ar.edu.um.turnos.appointment.shared.security.AuthenticatedUserProvider;
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

@WebMvcTest(controllers = ReservationController.class,
        properties = "app.jwt.secret=test-only-shared-secret-with-32-bytes-minimum")
@Import({JwtSecurityConfiguration.class, ReservationDtoMapper.class, ApiExceptionHandler.class,
        AuthenticatedUserProvider.class})
class ReservationControllerTest {

    private static final String SECRET = "test-only-shared-secret-with-32-bytes-minimum";
    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CreateReservationUseCase createReservation;
    @MockitoBean private ConfirmReservationUseCase confirmReservation;

    @Test
    void createsHoldWithoutAcceptingOwnerFromRequest() throws Exception {
        when(createReservation.create(OWNER, 15, LocalDate.parse("2026-10-26"), LocalTime.parse("09:00")))
                .thenReturn(new ReservationHold("hold-1", "process-1", "HELD",
                        Instant.parse("2026-10-26T10:00:00Z"), "private-payload"));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":15,"date":"2026-10-26","startTime":"09:00",
                                 "externalPatientId":"not-the-authenticated-user"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationProcessId").value("process-1"))
                .andExpect(jsonPath("$.holdId").value("hold-1"))
                .andExpect(jsonPath("$.status").value("HELD"))
                .andExpect(jsonPath("$.requestPayload").doesNotExist());
        verify(createReservation).create(OWNER, 15, LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"));
    }

    @Test
    void validatesCreateRequestAndUsesProblemJson() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + userToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(3));
    }

    @Test
    void confirmsProcessUsingNamesFromValidatedTokenClaims() throws Exception {
        when(confirmReservation.confirm("process-1", OWNER, "Juan", "Perez"))
                .thenReturn(new ReservationConfirmResult("process-1", "WAITING_FOR_PHONE",
                        Instant.parse("2026-10-26T10:00:00Z"), "private-payload"));

        mockMvc.perform(post("/api/reservations/process-1/confirm")
                        .header("Authorization", "Bearer " + userToken()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.reservationProcessId").value("process-1"))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"))
                .andExpect(jsonPath("$.requestPayload").doesNotExist());
        verify(confirmReservation).confirm("process-1", OWNER, "Juan", "Perez");
    }

    @Test
    void rejectsReservationApiWithoutUserJwt() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"professionalId":15,"date":"2026-10-26","startTime":"09:00"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private static String userToken() throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(JwtClaims.ISSUER)
                .subject("test.user")
                .issueTime(Date.from(now.minusSeconds(10)))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .claim(JwtClaims.PUBLIC_ID, OWNER.toString())
                .claim(JwtClaims.AUTHORITIES, List.of("ROLE_USER"))
                .claim(JwtClaims.FIRST_NAME, "Juan")
                .claim(JwtClaims.LAST_NAME, "Perez")
                .build();
        SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        token.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return token.serialize();
    }
}
