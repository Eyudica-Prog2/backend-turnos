package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import java.util.UUID;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;

public interface ConfirmReservationUseCase {

    ReservationConfirmResult confirm(String reservationProcessId, UUID ownerPublicId,
                                     String patientFirstName, String patientLastName);
}
