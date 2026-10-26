package ar.edu.um.turnos.appointment.reservation.infrastructure.rest;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.PhoneSubmissionConfigurationGateway;
import ar.edu.um.turnos.appointment.shared.domain.model.ReservationKafkaConfiguration;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.ReservationKafkaConfigurationGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import org.springframework.stereotype.Component;

@Component
public class PhoneSubmissionConfigurationGatewayAdapter implements PhoneSubmissionConfigurationGateway {

    private final ReservationKafkaConfigurationGateway integrationGateway;

    public PhoneSubmissionConfigurationGatewayAdapter(ReservationKafkaConfigurationGateway integrationGateway) {
        this.integrationGateway = integrationGateway;
    }

    @Override
    public PhoneSubmissionConfiguration currentConfiguration() {
        ReservationKafkaConfiguration integration = integrationGateway.currentConfiguration();
        if (!integration.provisioned()
                || blank(integration.groupId())
                || blank(integration.bootstrapServers())
                || blank(integration.phoneTopic())) {
            throw new CatedraClientException("CATEDRA_NOT_CONFIGURED", 503,
                    "The catedra phone topic is not provisioned for this project.");
        }
        return new PhoneSubmissionConfiguration(integration.groupId(),
                integration.bootstrapServers(), integration.phoneTopic());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
