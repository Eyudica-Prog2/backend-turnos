package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent;

public interface ProcessReservationEventUseCase {

    void process(ReservationEvent event);
}
