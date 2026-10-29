package ar.edu.um.turnos.appointment.reservation.domain.model;

import java.util.List;

/** A page returned by the catedra appointments API, already mapped to domain reservations. */
public record AppointmentListingPage(List<Reservation> reservations, long totalCount, String link) {

    public AppointmentListingPage {
        reservations = List.copyOf(reservations);
    }
}
