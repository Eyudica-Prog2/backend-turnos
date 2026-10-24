package ar.edu.um.turnos.appointment.availability.infrastructure.web.mapper;

import ar.edu.um.turnos.appointment.availability.domain.model.ProfessionalAvailability;
import ar.edu.um.turnos.appointment.availability.infrastructure.web.dto.ProfessionalAvailabilityResponse;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityDtoMapper {

    public ProfessionalAvailabilityResponse toResponse(ProfessionalAvailability availability) {
        return new ProfessionalAvailabilityResponse(
                availability.professionalId(),
                availability.date(),
                availability.slots().stream()
                        .map(slot -> new ProfessionalAvailabilityResponse.AvailableSlotResponse(
                                slot.startTime(), slot.endTime()))
                        .toList());
    }
}
