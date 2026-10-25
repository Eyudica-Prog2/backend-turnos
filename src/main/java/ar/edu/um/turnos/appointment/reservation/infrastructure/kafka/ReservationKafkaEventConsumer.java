package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ProcessReservationEventUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ReservationKafkaEventConsumer {

    private final ObjectMapper objectMapper;
    private final ProcessReservationEventUseCase processReservationEvent;
    private volatile String expectedGroupId;

    public ReservationKafkaEventConsumer(ObjectMapper objectMapper,
                                         ProcessReservationEventUseCase processReservationEvent) {
        this.objectMapper = objectMapper;
        this.processReservationEvent = processReservationEvent;
    }

    public void consume(String messageKey, String json) {
        try {
            ReservationKafkaEventMessage message = objectMapper.readValue(json, ReservationKafkaEventMessage.class);
            message.validateContract();
            if (messageKey == null || !messageKey.equals(message.reservationProcessId())) {
                throw new IllegalArgumentException("The Kafka message key must match reservationProcessId.");
            }
            if (expectedGroupId != null && !expectedGroupId.equals(message.groupId())) {
                throw new IllegalArgumentException("The Kafka event belongs to a different technical group.");
            }
            processReservationEvent.process(message.toDomain());
        } catch (JsonProcessingException malformedEvent) {
            throw new IllegalArgumentException("The reservation event is not valid JSON.", malformedEvent);
        }
    }

    void setExpectedGroupId(String groupId) {
        this.expectedGroupId = groupId;
    }
}
