package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import ar.edu.um.turnos.appointment.shared.domain.model.ReservationKafkaConfiguration;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.ReservationKafkaConfigurationGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhoneSubmissionConfigurationGatewayAdapterTest {

    @Mock private ReservationKafkaConfigurationGateway integrationGateway;

    @Test
    void returnsPhoneTopicSettingsFromTheProvisionedIntegration() {
        when(integrationGateway.currentConfiguration()).thenReturn(integration(true));
        PhoneSubmissionConfigurationGatewayAdapter adapter =
                new PhoneSubmissionConfigurationGatewayAdapter(integrationGateway);

        PhoneSubmissionConfiguration settings = adapter.currentConfiguration();

        assertEquals("group-1", settings.groupId());
        assertEquals("kafka.example.test:9092", settings.kafkaBootstrapServers());
        assertEquals("catedra.turnos.telefono.group-1", settings.kafkaTopic());
    }

    @Test
    void refusesToPublishWithoutProvisionedKafkaConfiguration() {
        when(integrationGateway.currentConfiguration()).thenReturn(integration(false));
        PhoneSubmissionConfigurationGatewayAdapter adapter =
                new PhoneSubmissionConfigurationGatewayAdapter(integrationGateway);

        CatedraClientException failure = assertThrows(
                CatedraClientException.class, adapter::currentConfiguration);

        assertEquals("CATEDRA_NOT_CONFIGURED", failure.getCode());
        assertEquals(503, failure.getHttpStatus());
    }

    private static ReservationKafkaConfiguration integration(boolean provisioned) {
        return new ReservationKafkaConfiguration("group-1", "kafka.example.test:9092",
                "consumer-group", "actions-topic", "catedra.turnos.telefono.group-1", provisioned);
    }
}
