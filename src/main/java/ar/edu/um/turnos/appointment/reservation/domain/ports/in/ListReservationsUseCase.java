package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import java.util.UUID;

public interface ListReservationsUseCase {

    ReservationPage list(UUID ownerPublicId, ReservationQuery query);
}
