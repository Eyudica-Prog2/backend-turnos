package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.shared.domain.model.ReservationKafkaConfiguration;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.ReservationKafkaConfigurationGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.MessageListenerContainer;

@ExtendWith(MockitoExtension.class)
class ReservationKafkaListenerManagerTest {

    @Mock private ReservationKafkaConfigurationGateway integrationGateway;
    @Mock private ReservationKafkaEventConsumer eventConsumer;
    @Mock private ReservationKafkaListenerFactory listenerFactory;
    @Mock private MessageListenerContainer container;

    @Test
    void retriesIntegrationLookupAfterStartupFailure() {
        ReservationKafkaConfiguration integration = provisionedConfiguration();
        when(integrationGateway.currentConfiguration())
                .thenThrow(new CatedraClientException("CATEDRA_UNAVAILABLE", 503, "unavailable"))
                .thenReturn(integration);
        when(listenerFactory.create(integration, eventConsumer)).thenReturn(container);

        ReservationKafkaListenerManager manager = manager();
        manager.run(null);

        verify(listenerFactory, never()).create(integration, eventConsumer);

        manager.recoverListener();

        verify(listenerFactory).create(integration, eventConsumer);
        verify(container).start();
        verify(eventConsumer).setExpectedGroupId("project-1");
    }

    @Test
    void recreatesAContainerStoppedAfterAnUnacknowledgedRecord() {
        ReservationKafkaConfiguration integration = provisionedConfiguration();
        MessageListenerContainer replacement = org.mockito.Mockito.mock(MessageListenerContainer.class);
        when(integrationGateway.currentConfiguration()).thenReturn(integration, integration);
        when(listenerFactory.create(integration, eventConsumer)).thenReturn(container, replacement);
        when(container.isRunning()).thenReturn(false);

        ReservationKafkaListenerManager manager = manager();
        manager.run(null);
        manager.recoverListener();

        verify(container).stop();
        verify(replacement).start();
    }

    private ReservationKafkaListenerManager manager() {
        return new ReservationKafkaListenerManager(integrationGateway, eventConsumer, listenerFactory);
    }

    private static ReservationKafkaConfiguration provisionedConfiguration() {
        return new ReservationKafkaConfiguration("project-1", "localhost:9092", "student-project-1",
                "student.appointments.actions", "course.appointments.phone", true);
    }
}
