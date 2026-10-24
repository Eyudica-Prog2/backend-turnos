package ar.edu.um.turnos.appointment.availability.domain.ports.out;

import ar.edu.um.turnos.appointment.availability.domain.model.CatalogProfessional;

public interface ProfessionalCatalogGateway {

    CatalogProfessional findProfessional(long professionalId);
}
