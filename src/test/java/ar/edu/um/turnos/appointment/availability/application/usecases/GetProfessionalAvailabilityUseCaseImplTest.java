package ar.edu.um.turnos.appointment.availability.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.AvailabilityQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyStatus;
import ar.edu.um.turnos.appointment.availability.domain.model.ProfessionalAvailability;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.AppointmentOccupancyGateway;
import ar.edu.um.turnos.appointment.availability.domain.model.CatalogProfessional;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklyScheduleQuery;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.ProfessionalCatalogGateway;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.WeeklyScheduleCatalogGateway;
import java.time.Clock;
import java.time.DayOfWeek;
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
class GetProfessionalAvailabilityUseCaseImplTest {

    private static final long PROFESSIONAL_ID = 15;
    private static final LocalDate DATE = LocalDate.parse("2026-09-14");

    @Mock
    private ProfessionalCatalogGateway professionalCatalogGateway;

    @Mock
    private WeeklyScheduleCatalogGateway weeklyScheduleCatalogGateway;

    @Mock
    private AppointmentOccupancyGateway appointmentOccupancyGateway;

    private Clock clock;
    private GetProfessionalAvailabilityUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-09-14T08:30:00Z"), ZoneOffset.UTC);
        useCase = new GetProfessionalAvailabilityUseCaseImpl(
                professionalCatalogGateway,
                weeklyScheduleCatalogGateway,
                appointmentOccupancyGateway,
                clock);
    }

    @Test
    void removesConfirmedAndHeldOccupanciesAndReturnsOnlyFutureSlots() {
        AvailabilityQuery query = new AvailabilityQuery(PROFESSIONAL_ID, DATE);
        when(professionalCatalogGateway.findProfessional(PROFESSIONAL_ID))
                .thenReturn(professional(true));
        when(weeklyScheduleCatalogGateway.findWeeklySchedules(new WeeklyScheduleQuery(PROFESSIONAL_ID, DATE)))
                .thenReturn(List.of(schedule("09:00", "12:00", 30)));
        when(appointmentOccupancyGateway.findOccupancies(new OccupancyQuery(PROFESSIONAL_ID, DATE, DATE)))
                .thenReturn(List.of(
                        new AppointmentOccupancy(DATE, LocalTime.parse("09:30"), OccupancyStatus.CONFIRMED, null),
                        new AppointmentOccupancy(DATE, LocalTime.parse("10:00"), OccupancyStatus.HELD,
                                Instant.parse("2026-09-14T08:45:00Z")),
                        new AppointmentOccupancy(DATE, LocalTime.parse("10:00"), OccupancyStatus.CONFIRMED, null)));

        ProfessionalAvailability result = useCase.getAvailability(query);

        assertThat(result.slots())
                .extracting(slot -> slot.startTime().toString())
                .containsExactly("09:00", "10:30", "11:00", "11:30");
    }

    @Test
    void excludesSlotsAtOrBeforeCurrentTimeOnToday() {
        Clock atNineThirty = Clock.fixed(Instant.parse("2026-09-14T09:30:00Z"), ZoneOffset.UTC);
        useCase = new GetProfessionalAvailabilityUseCaseImpl(
                professionalCatalogGateway,
                weeklyScheduleCatalogGateway,
                appointmentOccupancyGateway,
                atNineThirty);
        AvailabilityQuery query = new AvailabilityQuery(PROFESSIONAL_ID, DATE);
        when(professionalCatalogGateway.findProfessional(PROFESSIONAL_ID))
                .thenReturn(professional(true));
        when(weeklyScheduleCatalogGateway.findWeeklySchedules(new WeeklyScheduleQuery(PROFESSIONAL_ID, DATE)))
                .thenReturn(List.of(schedule("09:00", "10:30", 30)));
        when(appointmentOccupancyGateway.findOccupancies(new OccupancyQuery(PROFESSIONAL_ID, DATE, DATE)))
                .thenReturn(List.of());

        ProfessionalAvailability result = useCase.getAvailability(query);

        assertThat(result.slots()).extracting(slot -> slot.startTime().toString())
                .containsExactly("10:00");
    }

    @Test
    void returnsEmptyForDisabledProfessionalWithoutReadingSchedulesOrOccupancies() {
        when(professionalCatalogGateway.findProfessional(PROFESSIONAL_ID))
                .thenReturn(professional(false));

        ProfessionalAvailability result = useCase.getAvailability(new AvailabilityQuery(PROFESSIONAL_ID, DATE));

        assertThat(result.slots()).isEmpty();
        verify(weeklyScheduleCatalogGateway, never()).findWeeklySchedules(
                new WeeklyScheduleQuery(PROFESSIONAL_ID, DATE));
        verify(appointmentOccupancyGateway, never()).findOccupancies(
                new OccupancyQuery(PROFESSIONAL_ID, DATE, DATE));
    }

    @Test
    void doesNotQueryCatedraWhenCatalogHasNoSchedulesForTheDate() {
        when(professionalCatalogGateway.findProfessional(PROFESSIONAL_ID))
                .thenReturn(professional(true));
        when(weeklyScheduleCatalogGateway.findWeeklySchedules(new WeeklyScheduleQuery(PROFESSIONAL_ID, DATE)))
                .thenReturn(List.of());

        ProfessionalAvailability result = useCase.getAvailability(new AvailabilityQuery(PROFESSIONAL_ID, DATE));

        assertThat(result.slots()).isEmpty();
        verify(appointmentOccupancyGateway, never()).findOccupancies(
                new OccupancyQuery(PROFESSIONAL_ID, DATE, DATE));
    }

    @Test
    void returnsEmptyForPastDateAfterCheckingProfessionalExists() {
        Clock today = Clock.fixed(Instant.parse("2026-09-15T08:00:00Z"), ZoneOffset.UTC);
        useCase = new GetProfessionalAvailabilityUseCaseImpl(
                professionalCatalogGateway,
                weeklyScheduleCatalogGateway,
                appointmentOccupancyGateway,
                today);
        when(professionalCatalogGateway.findProfessional(PROFESSIONAL_ID))
                .thenReturn(professional(true));

        ProfessionalAvailability result = useCase.getAvailability(new AvailabilityQuery(PROFESSIONAL_ID, DATE));

        assertThat(result.slots()).isEmpty();
        verify(weeklyScheduleCatalogGateway, never()).findWeeklySchedules(
                new WeeklyScheduleQuery(PROFESSIONAL_ID, DATE));
    }

    private static CatalogProfessional professional(boolean enabled) {
        return new CatalogProfessional(PROFESSIONAL_ID, 2, "Ana", "Gomez", enabled);
    }

    private static WeeklySchedule schedule(String start, String end, int durationMinutes) {
        return new WeeklySchedule(DayOfWeek.MONDAY, LocalTime.parse(start), LocalTime.parse(end), durationMinutes);
    }
}
