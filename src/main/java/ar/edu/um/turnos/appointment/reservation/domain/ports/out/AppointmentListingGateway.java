package ar.edu.um.turnos.appointment.reservation.domain.ports.out;

import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentListingPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import java.util.UUID;

public interface AppointmentListingGateway {

    AppointmentListingPage list(UUID ownerPublicId, ReservationQuery query);
}
