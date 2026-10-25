package ar.edu.um.turnos.appointment.shared.domain.ports.out;

import ar.edu.um.turnos.appointment.shared.domain.model.AppointmentSlotAvailability;
import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentSlotAvailabilityGateway {

    AppointmentSlotAvailability inspect(long professionalId, LocalDate date, LocalTime startTime);
}
