package ar.edu.um.turnos.appointment.reservation.infrastructure.web.mapper;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessDetails;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto.PhoneSubmissionResponse;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto.ReservationHoldResponse;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto.ReservationProcessResponse;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto.ReservationResponse;
import org.springframework.stereotype.Component;

@Component
public class ReservationDtoMapper {

    public ReservationHoldResponse toResponse(ReservationHold hold) {
        return new ReservationHoldResponse(
                hold.reservationProcessId(), hold.holdId(), hold.status(), hold.expiresAt());
    }

    public PhoneSubmissionResponse toResponse(PhoneSubmissionResult result) {
        return new PhoneSubmissionResponse(result.reservationProcessId(), result.status(),
                result.phoneSubmissionStatus(), result.expiresAt());
    }

    public ReservationProcessResponse toResponse(ReservationProcessDetails details) {
        return new ReservationProcessResponse(details.reservationProcessId(), details.status(),
                details.expiresAt(), details.reservationId(), details.resultStatus(),
                details.error(), details.phoneSubmissionStatus());
    }

    public ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(reservation.reservationId(), reservation.reservationProcessId(),
                reservation.appointmentDate(), reservation.startTime(), reservation.endTime(),
                reservation.professionalId(), reservation.professionalFirstName(), reservation.professionalLastName(),
                reservation.patientFirstName(), reservation.patientLastName(), reservation.status(),
                reservation.createdAt(), reservation.confirmedAt(), reservation.cancelledAt(),
                reservation.cancellationReason());
    }
}
