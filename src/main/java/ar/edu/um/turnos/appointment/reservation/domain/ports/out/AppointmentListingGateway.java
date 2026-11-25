package ar.edu.um.turnos.appointment.reservation.domain.ports.out;

import java.util.UUID;

import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentListingPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;

public interface AppointmentListingGateway {

    AppointmentListingPage list(UUID ownerPublicId, ReservationQuery query);
}
