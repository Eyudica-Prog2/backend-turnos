package ar.edu.um.turnos.appointment.reservation.infrastructure.kafka;

import org.apache.kafka.clients.producer.Producer;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;

interface PhoneSubmissionKafkaProducerFactory {

    Producer<String, String> create(PhoneSubmissionConfiguration configuration);
}
