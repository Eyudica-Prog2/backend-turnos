package ar.edu.um.turnos.appointment.availability.domain.model;

import java.time.LocalDate;

public record WeeklyScheduleQuery(long professionalId, LocalDate date) {

    public WeeklyScheduleQuery {
        if (professionalId <= 0) {
            throw new IllegalArgumentException("Professional ID must be positive.");
        }
    }
}
