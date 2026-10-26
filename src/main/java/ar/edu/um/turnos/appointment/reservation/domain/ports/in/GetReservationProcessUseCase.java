package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessDetails;
import java.util.UUID;

public interface GetReservationProcessUseCase {

    ReservationProcessDetails get(String reservationProcessId, UUID ownerPublicId);
}
