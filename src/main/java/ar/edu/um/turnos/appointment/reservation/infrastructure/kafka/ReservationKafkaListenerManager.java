package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.domain.model.ReservationKafkaConfiguration;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.ReservationKafkaConfigurationGateway;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
public class ReservationKafkaListenerManager implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationKafkaListenerManager.class);

    private final ReservationKafkaConfigurationGateway integrationGateway;
    private final ReservationKafkaEventConsumer eventConsumer;
    private volatile MessageListenerContainer listenerContainer;

    public ReservationKafkaListenerManager(ReservationKafkaConfigurationGateway integrationGateway,
                                           ReservationKafkaEventConsumer eventConsumer) {
        this.integrationGateway = integrationGateway;
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ReservationKafkaConfiguration integration = integrationGateway.currentConfiguration();
            if (!integration.provisioned()) {
                LOGGER.warn("Reservation Kafka consumer was not started: catedra integration is not provisioned.");
                return;
            }
            start(integration);
        } catch (CatedraClientException unavailable) {
            LOGGER.warn("Reservation Kafka consumer was not started: integration lookup failed with code {}.",
                    unavailable.getCode());
        }
    }

    synchronized void start(ReservationKafkaConfiguration integration) {
        if (listenerContainer != null || integration.bootstrapServers() == null
                || integration.bootstrapServers().isBlank()
                || integration.actionsTopic() == null
                || integration.actionsTopic().isBlank()) {
            return;
        }
        eventConsumer.setExpectedGroupId(integration.groupId());

        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, integration.bootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, integration.consumerGroupId());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ContainerProperties containerProperties = new ContainerProperties(integration.actionsTopic());
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL);
        containerProperties.setMessageListener((AcknowledgingMessageListener<String, String>) (record, acknowledgement) -> {
            eventConsumer.consume(record.key(), record.value());
            acknowledgement.acknowledge();
        });
        KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(
                new DefaultKafkaConsumerFactory<>(consumerProperties), containerProperties);
        container.setCommonErrorHandler(new CommonContainerStoppingErrorHandler());
        container.setBeanName("reservation-actions-kafka-listener");
        container.start();
        listenerContainer = container;
        LOGGER.info("Reservation Kafka consumer started for configured actions topic.");
    }

    @PreDestroy
    public void stop() {
        MessageListenerContainer container = listenerContainer;
        if (container != null) {
            container.stop();
        }
    }
}
