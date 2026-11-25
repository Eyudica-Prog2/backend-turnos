package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;

public interface CreateReservationUseCase {

    ReservationHold create(UUID ownerPublicId, long professionalId, LocalDate date, LocalTime startTime);
}
