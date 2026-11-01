package ar.edu.um.turnos.appointment.reservation.application.usecases;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentCancellationResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.CancelReservationUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentCancellationGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CancelReservationUseCaseImpl implements CancelReservationUseCase {

    private final ReservationRepository reservations;
    private final AppointmentCancellationGateway appointmentCancellation;

    public CancelReservationUseCaseImpl(ReservationRepository reservations,
                                       AppointmentCancellationGateway appointmentCancellation) {
        this.reservations = reservations;
        this.appointmentCancellation = appointmentCancellation;
    }

    @Override
    public Reservation cancel(String reservationProcessId, UUID ownerPublicId, String reason) {
        if (reservationProcessId == null || reservationProcessId.isBlank() || ownerPublicId == null) {
            throw notFound();
        }
        Reservation reservation = reservations.findReservationByReservationProcessId(reservationProcessId)
                .orElseThrow(CancelReservationUseCaseImpl::notFound);
        if (!ownerPublicId.equals(reservation.ownerPublicId())) {
            throw notFound();
        }
        if (reason != null && reason.length() > 500) {
            throw new ReservationException("VALIDATION_ERROR", 400,
                    "The cancellation reason must not exceed 500 characters.");
        }
        if ("CANCELLED".equals(reservation.status())) {
            return reservation;
        }
        if (!"CONFIRMED".equals(reservation.status())) {
            throw notCancellable();
        }

        ReservationProcess process = reservations.findProcess(reservationProcessId).orElse(null);
        if (process != null && (!ownerPublicId.equals(process.ownerPublicId())
                || process.status() != ReservationProcessStatus.CONFIRMED)) {
            if (!ownerPublicId.equals(process.ownerPublicId())) {
                throw notFound();
            }
            throw notCancellable();
        }

        String normalizedReason = reason == null || reason.isBlank() ? null : reason.trim();
        AppointmentCancellationResult result = appointmentCancellation.cancel(reservationProcessId, normalizedReason);
        validateResult(reservation, reservationProcessId, result);

        Reservation cancelledReservation = reservation.cancelled(result.cancelledAt(), normalizedReason);
        ReservationProcess cancelledProcess = process == null ? null : process.cancelled(result.cancelledAt());
        reservations.saveCancellation(cancelledProcess, cancelledReservation);
        return cancelledReservation;
    }

    private static void validateResult(Reservation reservation, String requestedProcessId,
                                       AppointmentCancellationResult result) {
        if (result == null || !requestedProcessId.equals(result.reservationProcessId())
                || !"CANCELLED".equals(result.status()) || result.cancelledAt() == null
                || (reservation.reservationId() != null && result.reservationId() != null
                && !reservation.reservationId().equals(result.reservationId()))) {
            throw new CatedraClientException("CATEDRA_INVALID_RESPONSE", 503,
                    "The catedra cancellation response is empty or does not match the contract.");
        }
    }

    private static ReservationException notFound() {
        return new ReservationException("RESERVATION_NOT_FOUND", 404,
                "The reservation was not found.");
    }

    private static ReservationException notCancellable() {
        return new ReservationException("RESERVATION_NOT_CANCELLABLE", 409,
                "The reservation is not in a cancellable state.");
    }
}
