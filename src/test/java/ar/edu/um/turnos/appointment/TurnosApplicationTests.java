package ar.edu.um.turnos.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class TurnosApplicationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ReservationRepository reservations;

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USER", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("APP_JWT_SECRET", () -> "test-only-app-jwt-secret-with-minimum-length");
        registry.add("APP_TIME_ZONE", () -> "UTC");
        registry.add("CATEDRA_API_BASE_URL", () -> "http://localhost:9999");
        registry.add("CATEDRA_HTTP_CONNECT_TIMEOUT_MS", () -> "1000");
        registry.add("CATEDRA_HTTP_READ_TIMEOUT_MS", () -> "1000");
        registry.add("CATEDRA_RETRY_MAX_ATTEMPTS", () -> "1");
        registry.add("CATEDRA_RETRY_INITIAL_BACKOFF_MS", () -> "0");
        registry.add("CATEDRA_RETRY_MAX_BACKOFF_MS", () -> "0");
        registry.add("CATALOGO_API_BASE_URL", () -> "http://localhost:8081");
        registry.add("CATALOGO_HTTP_CONNECT_TIMEOUT_MS", () -> "1000");
        registry.add("CATALOGO_HTTP_READ_TIMEOUT_MS", () -> "1000");
    }

    @Test
    void applicationContextStartsWithPostgres() {
        // Starting the Spring context verifies datasource and Flyway configuration.
    }

    @Test
    void storesReservationProcessAndDeduplicatesEventsInPostgres() {
        Instant now = Instant.now();
        ReservationProcess process = reservations.saveProcess(ReservationProcess.created(
                UUID.randomUUID(), 15, LocalDate.parse("2027-01-04"),
                LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "{}", now));

        assertThat(process.id()).isNotNull();
        assertThat(process.status()).isEqualTo(ReservationProcessStatus.CREATED);
        assertThat(reservations.registerEventIfNew("event-test-1", "AdditionalInformationRequested", now)).isTrue();
        assertThat(reservations.registerEventIfNew("event-test-1", "AdditionalInformationRequested", now)).isFalse();
    }

    @Test
    void claimsPhoneSubmissionAtomicallyAndPersistsItsStatus() {
        Instant now = Instant.now();
        UUID owner = UUID.randomUUID();
        ReservationProcess process = reservations.saveProcess(ReservationProcess.created(
                owner, 15, LocalDate.parse("2027-01-04"), LocalTime.parse("09:00"),
                LocalTime.parse("09:30"), "Ana", "Gomez", "{}", now));
        process = process.held("process-phone-test", "hold-phone-test", now.plusSeconds(600), "{}", now)
                .waitingForPhone("Juan", "Perez", "{}", now.plusSeconds(600), now)
                .apply(new ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent(
                        "request-phone-test", "AdditionalInformationRequested", "process-phone-test",
                        "hold-phone-test", null, null, null, null, null, null, null, null,
                        now.plusSeconds(600), now, 1), now);
        reservations.saveProcess(process);
        Reservation reservation = new Reservation(null, process.id(), null, process.reservationProcessId(),
                owner, owner.toString(),
                15, LocalDate.parse("2027-01-04"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", null, PhoneSubmissionStatus.NOT_SENT,
                null, "PHONE_PENDING", now, null, null, null);
        reservations.saveReservation(reservation);

        Reservation pending = reservation.phoneSubmissionPending("+5492615551234", "event-phone-1");
        assertThat(reservations.beginPhoneSubmission(pending, "request-phone-test", now)).isTrue();
        assertThat(reservations.beginPhoneSubmission(
                reservation.phoneSubmissionPending("+5492615551234", "event-phone-2"),
                "request-phone-test", now)).isFalse();
        assertThat(reservations.markPhoneSubmissionSent(pending.phoneSubmissionSent("event-phone-1"))).isTrue();
        assertThat(reservations.findReservationByProcessId(process.id()))
                .get().extracting(Reservation::phoneSubmissionStatus)
                .isEqualTo(PhoneSubmissionStatus.SENT);
    }
}
