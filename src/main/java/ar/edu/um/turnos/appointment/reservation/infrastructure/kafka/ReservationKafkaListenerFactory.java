package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.shared.domain.model.ReservationKafkaConfiguration;

/** Creates the infrastructure container for the configured reservation-actions topic. */
@Component
public class ReservationKafkaListenerFactory {

    public MessageListenerContainer create(ReservationKafkaConfiguration integration,
                                           ReservationKafkaEventConsumer eventConsumer) {
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
        // Keep the failed record unacknowledged. The manager recreates the stopped container
        // on its next recovery tick, after the underlying dependency has had time to recover.
        container.setCommonErrorHandler(new CommonContainerStoppingErrorHandler());
        container.setBeanName("reservation-actions-kafka-listener");
        return container;
    }
}
