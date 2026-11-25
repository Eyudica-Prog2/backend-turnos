package ar.edu.um.turnos.appointment.shared.domain.model;

public record ReservationKafkaConfiguration(
        String groupId,
        String bootstrapServers,
        String consumerGroupId,
        String actionsTopic,
        String phoneTopic,
        boolean provisioned) {
}
