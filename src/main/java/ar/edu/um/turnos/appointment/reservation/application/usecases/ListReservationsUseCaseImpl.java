package ar.edu.um.turnos.appointment.reservation.application.usecases;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentListingPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ListReservationsUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentListingGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ListReservationsUseCaseImpl implements ListReservationsUseCase {

    private static final Set<String> ALLOWED_STATUSES = Set.of("PHONE_PENDING", "CONFIRMED", "CANCELLED", "FAILED");

    private final AppointmentListingGateway appointmentListing;
    private final ReservationRepository reservations;

    public ListReservationsUseCaseImpl(AppointmentListingGateway appointmentListing,
                                       ReservationRepository reservations) {
        this.appointmentListing = appointmentListing;
        this.reservations = reservations;
    }

    @Override
    public ReservationPage list(UUID ownerPublicId, ReservationQuery query) {
        validate(ownerPublicId, query);
        AppointmentListingPage remotePage = appointmentListing.list(ownerPublicId, query);
        String expectedPatientId = ownerPublicId.toString();
        ArrayList<Reservation> synchronizedReservations = new ArrayList<>();

        for (Reservation remote : remotePage.reservations()) {
            if (!expectedPatientId.equals(remote.externalPatientId())) {
                continue;
            }

            ReservationProcess process = remote.reservationProcessId() == null
                    ? null : reservations.findProcess(remote.reservationProcessId()).orElse(null);
            if (process != null && !ownerPublicId.equals(process.ownerPublicId())) {
                continue;
            }

            Reservation existing = reservations.findReservationByReservationId(remote.reservationId())
                    .orElseGet(() -> remote.reservationProcessId() == null ? null
                            : reservations.findReservationByReservationProcessId(remote.reservationProcessId())
                                    .orElse(null));
            if (existing != null && !ownerPublicId.equals(existing.ownerPublicId())) {
                continue;
            }

            Reservation synchronizedReservation = synchronize(remote, existing, process, ownerPublicId);
            synchronizedReservations.add(reservations.saveReservation(synchronizedReservation));
        }

        return new ReservationPage(synchronizedReservations, remotePage.totalCount(), remotePage.link());
    }

    private static Reservation synchronize(Reservation remote, Reservation existing,
                                           ReservationProcess process, UUID ownerPublicId) {
        return new Reservation(
                existing == null ? null : existing.id(),
                process == null ? (existing == null ? null : existing.processId()) : process.id(),
                remote.reservationId(),
                remote.reservationProcessId(),
                ownerPublicId,
                ownerPublicId.toString(),
                remote.professionalId(),
                remote.appointmentDate(),
                remote.startTime(),
                remote.endTime(),
                remote.professionalFirstName(),
                remote.professionalLastName(),
                remote.patientFirstName(),
                remote.patientLastName(),
                remote.patientPhone(),
                existing == null ? PhoneSubmissionStatus.NOT_SENT : existing.phoneSubmissionStatus(),
                existing == null ? null : existing.phoneSubmissionEventId(),
                remote.status(),
                remote.createdAt(),
                remote.confirmedAt(),
                remote.cancelledAt(),
                remote.cancellationReason());
    }

    private static void validate(UUID ownerPublicId, ReservationQuery query) {
        if (ownerPublicId == null || query == null || query.page() < 0 || query.size() < 1 || query.size() > 100) {
            throw new ReservationException("VALIDATION_ERROR", 400,
                    "A valid authenticated user and page between 0 and 100 items are required.");
        }
        if (query.status() != null && !query.status().isBlank()
                && !ALLOWED_STATUSES.contains(query.status())) {
            throw new ReservationException("VALIDATION_ERROR", 400, "The reservation status is invalid.");
        }
        if (query.from() != null && query.to() != null && query.from().isAfter(query.to())) {
            throw new ReservationException("VALIDATION_ERROR", 400,
                    "The from date must be before or equal to the to date.");
        }
    }
}
