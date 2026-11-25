package ar.edu.um.turnos.appointment.catedra.domain.model;

/** Technical integration settings returned by the catedra for this project. */
public record CatedraIntegration(
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
        return "CatedraIntegration[groupId=" + groupId
                + ", provisioningStatus=" + provisioningStatus
                + ", redisPassword=<redacted>]";
    }
}
