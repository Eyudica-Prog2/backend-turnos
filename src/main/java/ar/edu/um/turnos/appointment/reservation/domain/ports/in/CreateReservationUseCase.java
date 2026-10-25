package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface CreateReservationUseCase {

    ReservationHold create(UUID ownerPublicId, long professionalId, LocalDate date, LocalTime startTime);
}
