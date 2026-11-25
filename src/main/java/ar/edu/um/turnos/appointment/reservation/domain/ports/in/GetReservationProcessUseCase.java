package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import java.util.UUID;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessDetails;

public interface GetReservationProcessUseCase {

    ReservationProcessDetails get(String reservationProcessId, UUID ownerPublicId);
}
