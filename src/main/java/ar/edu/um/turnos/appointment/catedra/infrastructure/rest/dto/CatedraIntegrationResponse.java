package ar.edu.um.turnos.appointment.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraIntegrationResponse(
        String groupId,
        String redisHost,
        Integer redisPort,
        String redisUsername,
        String redisPassword,
        String redisReadNamespace,
        String redisWriteNamespace,
        String kafkaBootstrapServers,
        String kafkaConsumerGroupId,
        String kafkaCatalogTopic,
        String kafkaAppointmentActionsTopic,
        String kafkaAppointmentPhoneTopic,
        String provisioningStatus) {

    @Override
    public String toString() {
        return "CatedraIntegrationResponse[groupId=" + groupId
                + ", provisioningStatus=" + provisioningStatus
                + ", redisPassword=<redacted>]";
    }
}
