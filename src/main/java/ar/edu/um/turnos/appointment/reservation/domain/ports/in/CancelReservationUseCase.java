package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import java.util.UUID;

public interface CancelReservationUseCase {

    Reservation cancel(String reservationProcessId, UUID ownerPublicId, String reason);
}
