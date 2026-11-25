package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import java.util.UUID;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;

public interface ListReservationsUseCase {

    ReservationPage list(UUID ownerPublicId, ReservationQuery query);
}
