package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmission;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationPhoneSubmissionKafkaAdapterTest {

    private static final PhoneSubmissionConfiguration CONFIGURATION = new PhoneSubmissionConfiguration(
            "group-1", "kafka.example.test:9092", "catedra.turnos.telefono.group-1");

    @Mock private PhoneSubmissionKafkaProducerFactory producerFactory;
    @Mock private Producer<String, String> producer;
    private ReservationPhoneSubmissionKafkaAdapter adapter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        adapter = new ReservationPhoneSubmissionKafkaAdapter(objectMapper, producerFactory);
        when(producerFactory.create(CONFIGURATION)).thenReturn(producer);
        lenient().when(producer.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void publishesExactContractPayloadWithProcessAsMessageKey() throws Exception {
        PhoneSubmission submission = new PhoneSubmission("event-1", "process-1", "hold-1",
                "request-1", "+5492615551234", Instant.parse("2026-10-26T10:00:00Z"));

        adapter.publish(submission, CONFIGURATION);

        ArgumentCaptor<ProducerRecord<String, String>> record = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(producer).send(record.capture());
        assertEquals("catedra.turnos.telefono.group-1", record.getValue().topic());
        assertEquals("process-1", record.getValue().key());
        var json = new ObjectMapper().readTree(record.getValue().value());
        assertEquals("event-1", json.get("eventId").asText());
        assertEquals("AdditionalInformationSubmitted", json.get("eventType").asText());
        assertEquals("group-1", json.get("groupId").asText());
        assertEquals("hold-1", json.get("holdId").asText());
        assertEquals("request-1", json.get("requestEventId").asText());
        assertEquals("+5492615551234", json.get("information").get("phoneNumber").asText());
        assertEquals(1, json.get("schemaVersion").asInt());
        verify(producer).close(Duration.ofSeconds(1));
    }

    @Test
    void doesNotHideAnUnacknowledgedPublishAsSuccess() {
        when(producer.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new ExecutionException(new RuntimeException())));

        CatedraClientException failure = assertThrows(CatedraClientException.class,
                () -> adapter.publish(new PhoneSubmission("event-2", "process-1", "hold-1",
                        "request-1", "+5492615551234", Instant.now()), CONFIGURATION));

        assertEquals("CATEDRA_UNAVAILABLE", failure.getCode());
        verify(producer).close(Duration.ofSeconds(1));
    }
}
