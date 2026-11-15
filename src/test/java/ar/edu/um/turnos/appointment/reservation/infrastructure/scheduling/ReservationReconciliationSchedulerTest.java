package ar.edu.um.turnos.appointment.reservation.infrastructure.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationRecoveryReport;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.RecoverReservationProcessesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationReconciliationSchedulerTest {

    @Mock private RecoverReservationProcessesUseCase recoverReservationProcesses;

    @Test
    void scansPersistedProcessesWhenTheApplicationIsReady() {
        when(recoverReservationProcesses.reconcileExpiredProcesses())
                .thenReturn(new ReservationRecoveryReport(3, 1, 0, 1, 0, 0));
        ReservationReconciliationScheduler scheduler = new ReservationReconciliationScheduler(
                recoverReservationProcesses);

        scheduler.recoverOnStartup();

        verify(recoverReservationProcesses).reconcileExpiredProcesses();
    }

    @Test
    void repeatsTheScanOnTheConfiguredSchedule() {
        when(recoverReservationProcesses.reconcileExpiredProcesses())
                .thenReturn(new ReservationRecoveryReport(1, 0, 0, 0, 0, 0));
        ReservationReconciliationScheduler scheduler = new ReservationReconciliationScheduler(
                recoverReservationProcesses);

        scheduler.reconcileExpiredProcesses();

        verify(recoverReservationProcesses).reconcileExpiredProcesses();
    }
}
