package ar.edu.um.turnos.appointment.reservation.infrastructure.scheduling;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationRecoveryReport;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.RecoverReservationProcessesUseCase;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.reservation.reconciliation", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ReservationReconciliationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationReconciliationScheduler.class);

    private final RecoverReservationProcessesUseCase recoverReservationProcesses;
    private final AtomicBoolean running = new AtomicBoolean();

    public ReservationReconciliationScheduler(RecoverReservationProcessesUseCase recoverReservationProcesses) {
        this.recoverReservationProcesses = recoverReservationProcesses;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOnStartup() {
        runRecovery("startup");
    }

    @Scheduled(initialDelayString = "${app.reservation.reconciliation.initial-delay-ms:30000}",
            fixedDelayString = "${app.reservation.reconciliation.fixed-delay-ms:300000}")
    public void reconcileExpiredProcesses() {
        runRecovery("scheduled");
    }

    private void runRecovery(String trigger) {
        if (!running.compareAndSet(false, true)) {
            LOGGER.debug("Reservation recovery skipped because another scan is still running.");
            return;
        }
        try {
            ReservationRecoveryReport report = recoverReservationProcesses.reconcileExpiredProcesses();
            LOGGER.info("Reservation recovery completed: trigger={}, pending={}, examined={}, reconciled={}, "
                            + "expired={}, deferred={}, skipped={}", trigger, report.pendingProcesses(),
                    report.examined(), report.reconciled(), report.expired(), report.deferred(), report.skipped());
        } catch (RuntimeException failure) {
            LOGGER.error("Reservation recovery scan failed: trigger={}, type={}",
                    trigger, failure.getClass().getSimpleName());
        } finally {
            running.set(false);
        }
    }
}
