package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import java.util.UUID;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionResult;

public interface SubmitReservationPhoneUseCase {

    PhoneSubmissionResult submit(String reservationProcessId, UUID ownerPublicId, String phoneNumber);
}
