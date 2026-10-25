package ar.edu.um.turnos.appointment.reservation.infrastructure.web.mapper;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto.ReservationHoldResponse;
import org.springframework.stereotype.Component;

@Component
public class ReservationDtoMapper {

    public ReservationHoldResponse toResponse(ReservationHold hold) {
        return new ReservationHoldResponse(
                hold.reservationProcessId(), hold.holdId(), hold.status(), hold.expiresAt());
    }
}
