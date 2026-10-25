package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import ar.edu.um.turnos.appointment.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.appointment.catedra.domain.ports.out.CatedraIntegrationGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
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

    private final CatedraIntegrationGateway integrationGateway;
    private final ReservationKafkaEventConsumer eventConsumer;
    private volatile MessageListenerContainer listenerContainer;

    public ReservationKafkaListenerManager(CatedraIntegrationGateway integrationGateway,
                                           ReservationKafkaEventConsumer eventConsumer) {
        this.integrationGateway = integrationGateway;
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            CatedraIntegration integration = integrationGateway.fetchCurrentIntegration();
            if (!"PROVISIONED".equals(integration.provisioningStatus())) {
                LOGGER.warn("Reservation Kafka consumer was not started: catedra integration is not provisioned.");
                return;
            }
            start(integration);
        } catch (CatedraClientException unavailable) {
            LOGGER.warn("Reservation Kafka consumer was not started: integration lookup failed with code {}.",
                    unavailable.getCode());
        }
    }

    synchronized void start(CatedraIntegration integration) {
        if (listenerContainer != null || integration.kafkaBootstrapServers() == null
                || integration.kafkaBootstrapServers().isBlank()
                || integration.kafkaAppointmentActionsTopic() == null
                || integration.kafkaAppointmentActionsTopic().isBlank()) {
            return;
        }
        eventConsumer.setExpectedGroupId(integration.groupId());

        Map<String, Object> consumerProperties = new HashMap<>();
        consumerProperties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, integration.kafkaBootstrapServers());
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, integration.kafkaConsumerGroupId());
        consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ContainerProperties containerProperties = new ContainerProperties(integration.kafkaAppointmentActionsTopic());
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
