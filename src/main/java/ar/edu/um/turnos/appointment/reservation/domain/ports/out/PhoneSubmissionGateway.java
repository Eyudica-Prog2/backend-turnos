package ar.edu.um.turnos.appointment.reservation.domain.ports.out;

import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmission;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;

public interface PhoneSubmissionGateway {

    void publish(PhoneSubmission submission, PhoneSubmissionConfiguration configuration);
}
