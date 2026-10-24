package ar.edu.um.turnos.appointment.availability.application.usecases;

import ar.edu.um.turnos.appointment.availability.domain.WeeklyScheduleSlotGenerator;
import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentSlot;
import ar.edu.um.turnos.appointment.availability.domain.model.AvailabilityQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyStatus;
import ar.edu.um.turnos.appointment.availability.domain.model.ProfessionalAvailability;
import ar.edu.um.turnos.appointment.availability.domain.ports.in.GetProfessionalAvailabilityUseCase;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.AppointmentOccupancyGateway;
import ar.edu.um.turnos.appointment.availability.domain.model.CatalogProfessional;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklyScheduleQuery;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.ProfessionalCatalogGateway;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.WeeklyScheduleCatalogGateway;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class GetProfessionalAvailabilityUseCaseImpl implements GetProfessionalAvailabilityUseCase {

    private final ProfessionalCatalogGateway professionalCatalogGateway;
    private final WeeklyScheduleCatalogGateway weeklyScheduleCatalogGateway;
    private final AppointmentOccupancyGateway appointmentOccupancyGateway;
    private final Clock clock;

    public GetProfessionalAvailabilityUseCaseImpl(
            ProfessionalCatalogGateway professionalCatalogGateway,
            WeeklyScheduleCatalogGateway weeklyScheduleCatalogGateway,
            AppointmentOccupancyGateway appointmentOccupancyGateway,
            Clock clock) {
        this.professionalCatalogGateway = professionalCatalogGateway;
        this.weeklyScheduleCatalogGateway = weeklyScheduleCatalogGateway;
        this.appointmentOccupancyGateway = appointmentOccupancyGateway;
        this.clock = clock;
    }

    @Override
    public ProfessionalAvailability getAvailability(AvailabilityQuery query) {
        CatalogProfessional professional = professionalCatalogGateway.findProfessional(query.professionalId());
        if (!professional.enabled() || query.date().isBefore(LocalDate.now(clock))) {
            return emptyAvailability(query);
        }

        List<WeeklySchedule> schedules = weeklyScheduleCatalogGateway.findWeeklySchedules(
                new WeeklyScheduleQuery(query.professionalId(), query.date()));
        List<AppointmentSlot> generatedSlots = WeeklyScheduleSlotGenerator.generate(query.date(), schedules);
        if (generatedSlots.isEmpty()) {
            return emptyAvailability(query);
        }

        List<AppointmentOccupancy> occupancies = appointmentOccupancyGateway.findOccupancies(
                new OccupancyQuery(query.professionalId(), query.date(), query.date()));
        Map<OccupiedSlot, OccupancyStatus> occupiedSlots = new HashMap<>();
        for (AppointmentOccupancy occupancy : occupancies) {
            OccupiedSlot key = new OccupiedSlot(occupancy.date(), occupancy.startTime());
            occupiedSlots.merge(key, occupancy.status(), GetProfessionalAvailabilityUseCaseImpl::preferredStatus);
        }

        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        List<AppointmentSlot> availableSlots = generatedSlots.stream()
                .filter(slot -> slot.date().isAfter(today)
                        || (slot.date().equals(today) && slot.startTime().isAfter(now)))
                .filter(slot -> !occupiedSlots.containsKey(new OccupiedSlot(slot.date(), slot.startTime())))
                .toList();
        return new ProfessionalAvailability(query.professionalId(), query.date(), availableSlots);
    }

    private static ProfessionalAvailability emptyAvailability(AvailabilityQuery query) {
        return new ProfessionalAvailability(query.professionalId(), query.date(), List.of());
    }

    private static OccupancyStatus preferredStatus(OccupancyStatus current, OccupancyStatus incoming) {
        if (current == OccupancyStatus.CONFIRMED || incoming == OccupancyStatus.CONFIRMED) {
            return OccupancyStatus.CONFIRMED;
        }
        return OccupancyStatus.HELD;
    }

    private record OccupiedSlot(LocalDate date, LocalTime startTime) {
    }
}
