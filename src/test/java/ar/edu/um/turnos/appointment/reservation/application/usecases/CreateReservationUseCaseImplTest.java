package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentHoldGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.shared.domain.model.AppointmentSlotAvailability;
import ar.edu.um.turnos.appointment.shared.domain.model.SlotOccupancy;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.AppointmentSlotAvailabilityGateway;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateReservationUseCaseImplTest {

    private static final LocalDate DATE = LocalDate.parse("2026-10-26");
    private static final LocalTime START = LocalTime.parse("09:00");
    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    @Mock private AppointmentSlotAvailabilityGateway slotAvailability;
    @Mock private AppointmentHoldGateway catedra;
    @Mock private ReservationRepository reservations;

    private CreateReservationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        useCase = new CreateReservationUseCaseImpl(slotAvailability, catedra, reservations, clock);
        lenient().when(slotAvailability.inspect(15, DATE, START)).thenReturn(
                new AppointmentSlotAvailability("Ana", "Gomez", true,
                        LocalTime.parse("09:30"), SlotOccupancy.AVAILABLE, null));
        lenient().when(reservations.saveProcess(any(ReservationProcess.class))).thenAnswer(invocation -> {
            ReservationProcess process = invocation.getArgument(0);
            return process.id() == null ? process.withId(42L) : process;
        });
    }

    @Test
    void validatesCurrentCatalogAndCreatesAndPersistsTheRemoteHold() {
        ReservationHold hold = new ReservationHold(
                "hold-1", "process-1", "HELD", NOW.plusSeconds(600), "hold-payload");
        when(catedra.createHold(15, DATE, START, OWNER.toString())).thenReturn(hold);

        ReservationHold result = useCase.create(OWNER, 15, DATE, START);

        assertEquals("process-1", result.reservationProcessId());
        ArgumentCaptor<ReservationProcess> saved = ArgumentCaptor.forClass(ReservationProcess.class);
        verify(reservations, org.mockito.Mockito.times(2)).saveProcess(saved.capture());
        assertEquals(ReservationProcessStatus.CREATED, saved.getAllValues().get(0).status());
        assertEquals(ReservationProcessStatus.HELD, saved.getAllValues().get(1).status());
        assertEquals("hold-1", saved.getAllValues().get(1).holdId());
        assertEquals("hold-payload", saved.getAllValues().get(1).holdRequestPayload());
    }

    @Test
    void rejectsSlotOutsideCurrentScheduleWithoutCallingCatedra() {
        when(slotAvailability.inspect(15, DATE, LocalTime.parse("10:00"))).thenReturn(
                new AppointmentSlotAvailability("Ana", "Gomez", true, null, SlotOccupancy.AVAILABLE, null));
        assertEquals("INVALID_SLOT", assertThrows(ReservationException.class,
                () -> useCase.create(OWNER, 15, DATE, LocalTime.parse("10:00"))).getCode());
        verify(catedra, never()).createHold(anyLong(), any(LocalDate.class), any(LocalTime.class), any());
        verify(reservations, never()).saveProcess(any());
    }

    @Test
    void distinguishesConfirmedAndHeldOccupancies() {
        when(slotAvailability.inspect(15, DATE, START)).thenReturn(new AppointmentSlotAvailability(
                "Ana", "Gomez", true, LocalTime.parse("09:30"), SlotOccupancy.CONFIRMED, null));

        assertEquals("SLOT_ALREADY_RESERVED", assertThrows(ReservationException.class,
                () -> useCase.create(OWNER, 15, DATE, START)).getCode());
        verify(catedra, never()).createHold(anyLong(), any(LocalDate.class), any(LocalTime.class), any());
    }

    @Test
    void rejectsDisabledProfessionalBeforePersistingAnything() {
        when(slotAvailability.inspect(15, DATE, START)).thenReturn(new AppointmentSlotAvailability(
                "Ana", "Gomez", false, null, SlotOccupancy.AVAILABLE, null));

        assertEquals("PROFESSIONAL_DISABLED", assertThrows(ReservationException.class,
                () -> useCase.create(OWNER, 15, DATE, START)).getCode());
        verify(reservations, never()).saveProcess(any());
    }
}
