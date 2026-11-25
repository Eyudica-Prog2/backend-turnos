package ar.edu.um.turnos.appointment.availability.domain.ports.in;

import ar.edu.um.turnos.appointment.availability.domain.model.AvailabilityQuery;
import ar.edu.um.turnos.appointment.availability.domain.model.ProfessionalAvailability;

public interface GetProfessionalAvailabilityUseCase {

    ProfessionalAvailability getAvailability(AvailabilityQuery query);
}
