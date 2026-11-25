package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import java.util.UUID;

import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;

public interface CancelReservationUseCase {

    Reservation cancel(String reservationProcessId, UUID ownerPublicId, String reason);
}
