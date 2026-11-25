package ar.edu.um.turnos.appointment.shared.domain.ports.out;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.turnos.appointment.shared.domain.model.AppointmentSlotAvailability;

public interface AppointmentSlotAvailabilityGateway {

    AppointmentSlotAvailability inspect(long professionalId, LocalDate date, LocalTime startTime);
}
