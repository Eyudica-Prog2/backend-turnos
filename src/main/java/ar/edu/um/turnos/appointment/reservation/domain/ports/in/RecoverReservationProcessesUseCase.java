package ar.edu.um.turnos.appointment.reservation.domain.ports.in;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationRecoveryReport;

public interface RecoverReservationProcessesUseCase {

    ReservationRecoveryReport reconcileExpiredProcesses();
}
