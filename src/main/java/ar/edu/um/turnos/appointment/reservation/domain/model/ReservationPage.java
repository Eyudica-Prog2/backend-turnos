package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.util.List;

public record ReservationPage(List<Reservation> reservations, long totalCount, String link) {

    public ReservationPage {
        reservations = List.copyOf(reservations);
    }
}
