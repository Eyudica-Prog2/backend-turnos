package ar.edu.um.turnos.appointment.catedra.infrastructure.rest.mapper;

import ar.edu.um.turnos.appointment.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.appointment.catedra.infrastructure.rest.dto.CatedraIntegrationResponse;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CatedraIntegrationResponseMapper {

    private static final Set<String> PROVISIONING_STATUSES = Set.of("PENDING", "PROVISIONED", "FAILED", "REVOKED");

    public CatedraIntegration toDomain(CatedraIntegrationResponse response) {
        if (!isUsable(response)) {
            throw new IllegalArgumentException("Catedra integration response does not match the contract.");
        }
        return new CatedraIntegration(
                response.groupId(),
                response.redisHost(),
                response.redisPort(),
                response.redisUsername(),
                response.redisPassword(),
                response.redisReadNamespace(),
                response.redisWriteNamespace(),
                response.kafkaBootstrapServers(),
                response.kafkaConsumerGroupId(),
                response.kafkaCatalogTopic(),
                response.kafkaAppointmentActionsTopic(),
                response.kafkaAppointmentPhoneTopic(),
                response.provisioningStatus());
    }

    public boolean isUsable(CatedraIntegrationResponse response) {
        if (response == null
                || !hasText(response.groupId())
                || !hasText(response.redisHost())
                || response.redisPort() == null
                || response.redisPort() < 1
                || !hasText(response.redisUsername())
                || !hasText(response.redisReadNamespace())
                || !hasText(response.redisWriteNamespace())
                || !hasText(response.kafkaBootstrapServers())
                || !hasText(response.kafkaConsumerGroupId())
                || !hasText(response.kafkaCatalogTopic())
                || !hasText(response.kafkaAppointmentActionsTopic())
                || !hasText(response.kafkaAppointmentPhoneTopic())
                || !PROVISIONING_STATUSES.contains(response.provisioningStatus())) {
            return false;
        }
        return !"PROVISIONED".equals(response.provisioningStatus()) || hasText(response.redisPassword());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
