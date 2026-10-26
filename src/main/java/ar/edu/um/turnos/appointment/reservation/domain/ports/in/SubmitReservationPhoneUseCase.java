package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionResult;
import java.util.UUID;

public interface SubmitReservationPhoneUseCase {

    PhoneSubmissionResult submit(String reservationProcessId, UUID ownerPublicId, String phoneNumber);
}
