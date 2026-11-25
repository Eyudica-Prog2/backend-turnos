package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ProcessReservationEventUseCase;

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
            // To ReservationKafkaEventMessage
            ReservationKafkaEventMessage message = objectMapper.readValue(json, ReservationKafkaEventMessage.class);
            message.validateContract();
            if (messageKey == null || !messageKey.equals(message.reservationProcessId())) {
                throw new IllegalArgumentException("The Kafka message key must match reservationProcessId.");
            }
            // AdditionalInformationRequested is scoped by its configured topic; contract
            // section 15.4 does not require groupId in that event's payload. When a payload
            // does carry groupId, still reject a message for a different technical group.
            if (expectedGroupId != null && message.groupId() != null
                    && !expectedGroupId.equals(message.groupId())) {
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
