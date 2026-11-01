package ar.edu.um.turnos.appointment.reservation.domain.ports.out;

import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentCancellationResult;

public interface AppointmentCancellationGateway {

    AppointmentCancellationResult cancel(String reservationProcessId, String reason);
}
