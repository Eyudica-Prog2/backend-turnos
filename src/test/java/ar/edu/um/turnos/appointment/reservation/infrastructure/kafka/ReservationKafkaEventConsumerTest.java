package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import static org.mockito.Mockito.verify;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ProcessReservationEventUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationKafkaEventConsumerTest {

    @Mock private ProcessReservationEventUseCase processEvent;

    @Test
    void mapsContractDateFieldAndIgnoresUnknownFields() {
        ReservationKafkaEventConsumer consumer = new ReservationKafkaEventConsumer(
                new ObjectMapper().findAndRegisterModules(), processEvent);

        consumer.consume("process-1", """
                {
                  "eventId":"event-1",
                  "eventType":"AppointmentConfirmed",
                  "groupId":"project-1",
                  "reservationProcessId":"process-1",
                  "holdId":"hold-1",
                  "requestEventId":"request-1",
                  "reservationId":8251,
                  "professionalId":15,
                  "date":"2026-10-26",
                  "startTime":"09:30",
                  "status":"CONFIRMED",
                  "occurredAt":"2026-10-25T12:00:00Z",
                  "schemaVersion":1,
                  "futureOptionalField":"ignored"
                }
                """);

        verify(processEvent).process(new ReservationEvent("event-1", "AppointmentConfirmed",
                "process-1", "hold-1", "request-1", 8251L, 15L, "2026-10-26", "09:30",
                "CONFIRMED", null, null, null, Instant.parse("2026-10-25T12:00:00Z"), 1));
    }

    @Test
    void rejectsMessageKeyThatDoesNotMatchTheProcessId() {
        ReservationKafkaEventConsumer consumer = new ReservationKafkaEventConsumer(
                new ObjectMapper().findAndRegisterModules(), processEvent);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> consumer.consume("wrong-process", """
                        {"eventId":"event-1","eventType":"AppointmentProcessExpired",
                         "groupId":"project-1","reservationProcessId":"process-1",
                         "holdId":"hold-1","requestEventId":"request-1",
                         "reason":"HOLD_EXPIRED","occurredAt":"2026-10-25T12:00:00Z",
                         "schemaVersion":1}
                        """));
        org.mockito.Mockito.verifyNoInteractions(processEvent);
    }
}
