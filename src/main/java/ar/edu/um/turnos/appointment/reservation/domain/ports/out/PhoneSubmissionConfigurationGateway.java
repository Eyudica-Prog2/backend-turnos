package ar.edu.um.turnos.appointment.reservation.domain.ports.out;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;

public interface PhoneSubmissionConfigurationGateway {

    PhoneSubmissionConfiguration currentConfiguration();
}
