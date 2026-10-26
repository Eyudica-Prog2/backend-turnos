package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmission;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.PhoneSubmissionGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

@Component
public class ReservationPhoneSubmissionKafkaAdapter implements PhoneSubmissionGateway {

    private final ObjectMapper objectMapper;
    private final PhoneSubmissionKafkaProducerFactory producerFactory;

    public ReservationPhoneSubmissionKafkaAdapter(ObjectMapper objectMapper,
                                                  PhoneSubmissionKafkaProducerFactory producerFactory) {
        this.objectMapper = objectMapper;
        this.producerFactory = producerFactory;
    }

    @Override
    public void publish(PhoneSubmission submission, PhoneSubmissionConfiguration configuration) {
        String payload = serialize(new AdditionalInformationSubmitted(
                submission.eventId(), "AdditionalInformationSubmitted", configuration.groupId(),
                submission.reservationProcessId(), submission.holdId(), submission.requestEventId(),
                new Information(submission.phoneNumber()), submission.occurredAt(), 1));

        Producer<String, String> producer = null;
        try {
            producer = producerFactory.create(configuration);
            producer.send(new ProducerRecord<>(configuration.kafkaTopic(),
                    submission.reservationProcessId(), payload)).get(12, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (ExecutionException | TimeoutException | RuntimeException publicationFailure) {
            throw unavailable();
        } finally {
            if (producer != null) {
                try {
                    producer.close(Duration.ofSeconds(1));
                } catch (RuntimeException ignoredCloseFailure) {
                    // The broker acknowledgement is authoritative; closing the local client is best effort.
                }
            }
        }
    }

    private String serialize(AdditionalInformationSubmitted message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException serializationFailure) {
            throw new CatedraClientException(
                    "CATEDRA_INVALID_RESPONSE", 503,
                    "The phone submission event could not be serialized.");
        }
    }

    private static CatedraClientException unavailable() {
        return new CatedraClientException(
                "CATEDRA_UNAVAILABLE", 503,
                "The phone submission was not acknowledged; do not resend it blindly.");
    }

    private record AdditionalInformationSubmitted(
            String eventId,
            String eventType,
            String groupId,
            String reservationProcessId,
            String holdId,
            String requestEventId,
            Information information,
            java.time.Instant occurredAt,
            int schemaVersion) {
    }

    private record Information(String phoneNumber) {
    }
}
