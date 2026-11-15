package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.domain.model.ReservationKafkaConfiguration;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.ReservationKafkaConfigurationGateway;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationKafkaListenerManager implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationKafkaListenerManager.class);

    private final ReservationKafkaConfigurationGateway integrationGateway;
    private final ReservationKafkaEventConsumer eventConsumer;
    private final ReservationKafkaListenerFactory listenerFactory;
    private volatile MessageListenerContainer listenerContainer;

    public ReservationKafkaListenerManager(ReservationKafkaConfigurationGateway integrationGateway,
                                           ReservationKafkaEventConsumer eventConsumer,
                                           ReservationKafkaListenerFactory listenerFactory) {
        this.integrationGateway = integrationGateway;
        this.eventConsumer = eventConsumer;
        this.listenerFactory = listenerFactory;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureStarted("startup");
    }

    /**
     * Retries integration discovery after startup failures and replaces containers stopped
     * by a processing exception. Kafka's own network client continues reconnecting while an
     * existing container remains active; this tick deals with failures that stopped it.
     */
    @Scheduled(initialDelayString = "${app.kafka.listener.recovery-initial-delay-ms:30000}",
            fixedDelayString = "${app.kafka.listener.recovery-fixed-delay-ms:30000}")
    public void recoverListener() {
        ensureStarted("scheduled-recovery");
    }

    synchronized void ensureStarted(String trigger) {
        MessageListenerContainer current = listenerContainer;
        if (current != null && current.isRunning()) {
            return;
        }
        if (current != null) {
            LOGGER.warn("Reservation Kafka consumer stopped; recreating it on recovery trigger {}.", trigger);
            current.stop();
            listenerContainer = null;
        }

        try {
            ReservationKafkaConfiguration integration = integrationGateway.currentConfiguration();
            if (!integration.provisioned()) {
                LOGGER.warn("Reservation Kafka consumer is not provisioned; next recovery trigger is {}.", trigger);
                return;
            }
            if (isBlank(integration.groupId()) || isBlank(integration.bootstrapServers())
                    || isBlank(integration.consumerGroupId()) || isBlank(integration.actionsTopic())) {
                LOGGER.warn("Reservation Kafka consumer configuration is incomplete; next recovery trigger is {}.",
                        trigger);
                return;
            }

            eventConsumer.setExpectedGroupId(integration.groupId());
            MessageListenerContainer next = listenerFactory.create(integration, eventConsumer);
            try {
                next.start();
                listenerContainer = next;
                LOGGER.info("Reservation Kafka consumer started for configured actions topic; trigger={}", trigger);
            } catch (RuntimeException startFailure) {
                next.stop();
                throw startFailure;
            }
        } catch (CatedraClientException unavailable) {
            LOGGER.warn("Reservation Kafka consumer is unavailable; trigger={}, code={}.",
                    trigger, unavailable.getCode());
        } catch (RuntimeException failure) {
            LOGGER.error("Reservation Kafka consumer could not start; trigger={}, type={}; it will be retried.",
                    trigger, failure.getClass().getSimpleName());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @PreDestroy
    public synchronized void stop() {
        MessageListenerContainer container = listenerContainer;
        if (container != null) {
            container.stop();
            listenerContainer = null;
        }
    }
}
