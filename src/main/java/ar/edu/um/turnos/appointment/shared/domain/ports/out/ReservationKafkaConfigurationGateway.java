package ar.edu.um.turnos.appointment.shared.domain.ports.out;

import ar.edu.um.turnos.appointment.shared.domain.model.ReservationKafkaConfiguration;

public interface ReservationKafkaConfigurationGateway {

    ReservationKafkaConfiguration currentConfiguration();
}
