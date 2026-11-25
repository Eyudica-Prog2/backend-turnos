package ar.edu.um.turnos.appointment.catedra.domain.ports.out;

import ar.edu.um.turnos.appointment.catedra.domain.model.CatedraIntegration;

public interface CatedraIntegrationGateway {

    CatedraIntegration fetchCurrentIntegration();
}
