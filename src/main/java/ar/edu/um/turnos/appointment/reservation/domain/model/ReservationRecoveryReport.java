package ar.edu.um.turnos.appointment.reservation.domain.model;

public record ReservationRecoveryReport(
        long pendingProcesses,
        int examined,
        int reconciled,
        int expired,
        int deferred,
        int skipped) {
}
