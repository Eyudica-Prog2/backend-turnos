package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.time.LocalDate;

public record ReservationQuery(String status, LocalDate from, LocalDate to, int page, int size) {
}
