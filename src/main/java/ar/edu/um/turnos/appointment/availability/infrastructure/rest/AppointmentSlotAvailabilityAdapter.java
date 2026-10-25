package ar.edu.um.turnos.appointment.availability.infrastructure.rest;

import ar.edu.um.turnos.appointment.availability.domain.WeeklyScheduleSlotGenerator;
import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentOccupancy;
import ar.edu.um.turnos.appointment.availability.domain.model.AppointmentSlot;
import ar.edu.um.turnos.appointment.availability.domain.model.CatalogProfessional;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.OccupancyStatus;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklySchedule;
import ar.edu.um.turnos.appointment.availability.domain.model.WeeklyScheduleQuery;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.AppointmentOccupancyGateway;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.ProfessionalCatalogGateway;
import ar.edu.um.turnos.appointment.availability.domain.ports.out.WeeklyScheduleCatalogGateway;
import ar.edu.um.turnos.appointment.shared.domain.model.AppointmentSlotAvailability;
import ar.edu.um.turnos.appointment.shared.domain.model.SlotOccupancy;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.AppointmentSlotAvailabilityGateway;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Component;

/** Bridges the current-catalog/occupancy feature to the reservation use case via a shared port. */
@Component
public class AppointmentSlotAvailabilityAdapter implements AppointmentSlotAvailabilityGateway {

    private final ProfessionalCatalogGateway professionals;
    private final WeeklyScheduleCatalogGateway schedules;
    private final AppointmentOccupancyGateway occupancies;
    private final Clock clock;

    public AppointmentSlotAvailabilityAdapter(ProfessionalCatalogGateway professionals,
                                              WeeklyScheduleCatalogGateway schedules,
                                              AppointmentOccupancyGateway occupancies,
                                              Clock clock) {
        this.professionals = professionals;
        this.schedules = schedules;
        this.occupancies = occupancies;
        this.clock = clock;
    }

    @Override
    public AppointmentSlotAvailability inspect(long professionalId, LocalDate date, LocalTime startTime) {
        CatalogProfessional professional = professionals.findProfessional(professionalId);
        if (!professional.enabled()) {
            return new AppointmentSlotAvailability(professional.firstName(), professional.lastName(),
                    false, null, SlotOccupancy.AVAILABLE, null);
        }

        List<WeeklySchedule> currentSchedules = schedules.findWeeklySchedules(
                new WeeklyScheduleQuery(professionalId, date));
        AppointmentSlot slot = WeeklyScheduleSlotGenerator.generate(date, currentSchedules).stream()
                .filter(candidate -> candidate.startTime().equals(startTime))
                .findFirst().orElse(null);
        if (slot == null) {
            return new AppointmentSlotAvailability(professional.firstName(), professional.lastName(),
                    true, null, SlotOccupancy.AVAILABLE, null);
        }

        List<AppointmentOccupancy> matching = occupancies.findOccupancies(
                        new OccupancyQuery(professionalId, date, date)).stream()
                .filter(occupancy -> occupancy.date().equals(date)
                        && occupancy.startTime().equals(startTime))
                .toList();
        boolean confirmed = matching.stream().anyMatch(item -> item.status() == OccupancyStatus.CONFIRMED);
        if (confirmed) {
            return new AppointmentSlotAvailability(professional.firstName(), professional.lastName(),
                    true, slot.endTime(), SlotOccupancy.CONFIRMED, null);
        }
        AppointmentOccupancy activeHold = matching.stream()
                .filter(item -> item.status() == OccupancyStatus.HELD
                        && item.expiresAt().isAfter(clock.instant()))
                .findFirst().orElse(null);
        return new AppointmentSlotAvailability(professional.firstName(), professional.lastName(),
                true, slot.endTime(), activeHold == null ? SlotOccupancy.AVAILABLE : SlotOccupancy.HELD,
                activeHold == null ? null : activeHold.expiresAt());
    }
}
