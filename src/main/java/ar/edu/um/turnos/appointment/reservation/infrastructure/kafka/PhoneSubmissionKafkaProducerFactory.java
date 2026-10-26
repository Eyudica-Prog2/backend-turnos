package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;
import org.apache.kafka.clients.producer.Producer;

interface PhoneSubmissionKafkaProducerFactory {

    Producer<String, String> create(PhoneSubmissionConfiguration configuration);
}
