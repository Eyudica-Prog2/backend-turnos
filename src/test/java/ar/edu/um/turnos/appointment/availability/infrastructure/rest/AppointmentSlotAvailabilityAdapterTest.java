package ar.edu.um.turnos.appointment.availability.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.CatalogProfessional;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyStatus;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklyScheduleQuery;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.AppointmentOccupancyGateway;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.ProfessionalCatalogGateway;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.WeeklyScheduleCatalogGateway;
import ar.edu.um.turnos.appointment.shared.domain.model.SlotOccupancy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppointmentSlotAvailabilityAdapterTest {

    private static final LocalDate DATE = LocalDate.parse("2026-10-26");
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    @Mock private ProfessionalCatalogGateway professionals;
    @Mock private WeeklyScheduleCatalogGateway schedules;
    @Mock private AppointmentOccupancyGateway occupancies;
    private AppointmentSlotAvailabilityAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AppointmentSlotAvailabilityAdapter(professionals, schedules, occupancies,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(professionals.findProfessional(15)).thenReturn(
                new CatalogProfessional(15, 1, "Ana", "Gomez", true));
        when(schedules.findWeeklySchedules(new WeeklyScheduleQuery(15, DATE))).thenReturn(List.of(
                new WeeklySchedule(java.time.DayOfWeek.MONDAY,
                        LocalTime.parse("09:00"), LocalTime.parse("10:00"), 30)));
    }

    @Test
    void returnsTheCurrentCatalogSlotAndConfirmedOccupancyPrecedence() {
        when(occupancies.findOccupancies(new OccupancyQuery(15, DATE, DATE))).thenReturn(List.of(
                new AppointmentOccupancy(DATE, LocalTime.parse("09:00"),
                        OccupancyStatus.HELD, NOW.plusSeconds(300)),
                new AppointmentOccupancy(DATE, LocalTime.parse("09:00"),
                        OccupancyStatus.CONFIRMED, null)));

        var slot = adapter.inspect(15, DATE, LocalTime.parse("09:00"));

        assertEquals("Ana", slot.professionalFirstName());
        assertEquals(LocalTime.parse("09:30"), slot.endTime());
        assertEquals(SlotOccupancy.CONFIRMED, slot.occupancy());
    }

    @Test
    void expiredHoldDoesNotBlockTheSlot() {
        when(occupancies.findOccupancies(new OccupancyQuery(15, DATE, DATE))).thenReturn(List.of(
                new AppointmentOccupancy(DATE, LocalTime.parse("09:00"),
                        OccupancyStatus.HELD, NOW.minusSeconds(1))));

        var slot = adapter.inspect(15, DATE, LocalTime.parse("09:00"));

        assertEquals(SlotOccupancy.AVAILABLE, slot.occupancy());
    }

    @Test
    void doesNotQueryOccupanciesForATimeOutsideTheCurrentSchedule() {
        var slot = adapter.inspect(15, DATE, LocalTime.parse("10:00"));

        assertEquals(null, slot.endTime());
        verify(occupancies, never()).findOccupancies(org.mockito.ArgumentMatchers.any());
    }
}
