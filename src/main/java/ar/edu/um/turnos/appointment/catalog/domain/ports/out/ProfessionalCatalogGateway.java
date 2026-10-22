package ar.edu.um.turnos.appointment.catalog.domain.ports.out;

import ar.edu.um.turnos.appointment.catalog.domain.model.CatalogProfessional;

public interface ProfessionalCatalogGateway {

    CatalogProfessional findProfessional(long professionalId);
}
