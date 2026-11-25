package ar.edu.um.turnos.appointment.reservation.domain.model;

public record PhoneSubmissionConfiguration(
        String groupId,
        String kafkaBootstrapServers,
        String kafkaTopic) {
}
