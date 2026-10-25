package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import java.util.UUID;

public interface ConfirmReservationUseCase {

    ReservationConfirmResult confirm(String reservationProcessId, UUID ownerPublicId,
                                     String patientFirstName, String patientLastName);
}
