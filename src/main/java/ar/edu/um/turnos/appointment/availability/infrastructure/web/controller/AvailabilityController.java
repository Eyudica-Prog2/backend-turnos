package ar.edu.um.turnos.appointment.availability.infrastructure.web.controller;

import ar.edu.um.turnos.appointment.availability.domain.model.AvailabilityQuery;
import ar.edu.um.turnos.appointment.availability.domain.ports.in.GetProfessionalAvailabilityUseCase;
import ar.edu.um.turnos.appointment.availability.infrastructure.web.dto.ProfessionalAvailabilityResponse;
import ar.edu.um.turnos.appointment.availability.infrastructure.web.mapper.AvailabilityDtoMapper;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AvailabilityController {

    private final GetProfessionalAvailabilityUseCase getAvailability;
    private final AvailabilityDtoMapper dtoMapper;

    public AvailabilityController(GetProfessionalAvailabilityUseCase getAvailability,
                                  AvailabilityDtoMapper dtoMapper) {
        this.getAvailability = getAvailability;
        this.dtoMapper = dtoMapper;
    }

    @GetMapping("/api/professionals/{id}/availability")
    public ResponseEntity<ProfessionalAvailabilityResponse> getAvailability(
            @PathVariable("id") long professionalId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AvailabilityQuery query = new AvailabilityQuery(professionalId, date);
        return ResponseEntity.ok(dtoMapper.toResponse(getAvailability.getAvailability(query)));
    }
}
