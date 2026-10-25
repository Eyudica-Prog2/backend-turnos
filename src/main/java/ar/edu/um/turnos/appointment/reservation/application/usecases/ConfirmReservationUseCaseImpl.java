package ar.edu.um.turnos.appointment.reservation.application.usecases;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ConfirmReservationUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentHoldGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConfirmReservationUseCaseImpl implements ConfirmReservationUseCase {

    private final ReservationRepository reservations;
    private final AppointmentHoldGateway catedra;
    private final Clock clock;

    public ConfirmReservationUseCaseImpl(ReservationRepository reservations,
                                        AppointmentHoldGateway catedra,
                                        Clock clock) {
        this.reservations = reservations;
        this.catedra = catedra;
        this.clock = clock;
    }

    @Override
    public ReservationConfirmResult confirm(String processId, UUID ownerPublicId,
                                             String patientFirstName, String patientLastName) {
        ReservationProcess process = reservations.findProcess(processId)
                .filter(candidate -> candidate.ownerPublicId().equals(ownerPublicId))
                .orElseThrow(() -> new ReservationException(
                        "HOLD_NOT_FOUND", 404, "The reservation hold was not found."));

        if (process.status() == ReservationProcessStatus.WAITING_FOR_PHONE
                || process.status() == ReservationProcessStatus.CONFIRMED
                || process.status() == ReservationProcessStatus.REJECTED_RETRYABLE) {
            throw new ReservationException(
                    "RESERVATION_PROCESS_ALREADY_CONFIRMED", 409,
                    "The reservation process has already been confirmed.");
        }
        if (process.status() != ReservationProcessStatus.HELD || process.holdId() == null) {
            throw new ReservationException("HOLD_NOT_FOUND", 404, "The reservation hold was not found.");
        }
        if (process.expiresAt() == null || !process.expiresAt().isAfter(clock.instant())) {
            throw new ReservationException("HOLD_EXPIRED", 404, "The reservation hold has expired.");
        }
        if (isInvalidName(patientFirstName) || isInvalidName(patientLastName)) {
            throw new ReservationException("USER_PROFILE_CLAIMS_MISSING", 503,
                    "The validated user token does not contain the required firstName and lastName claims.");
        }

        ReservationConfirmResult response = catedra.confirmHold(
                process.holdId(), process.reservationProcessId(), process.ownerPublicId().toString(),
                patientFirstName, patientLastName);
        if (response == null || !process.reservationProcessId().equals(response.reservationProcessId())
                || !"WAITING_FOR_PHONE".equals(response.status())) {
            throw new ReservationException("CATEDRA_INVALID_RESPONSE", 503,
                    "The catedra confirmation response does not match the expected process.");
        }

        Instant now = clock.instant();
        ReservationProcess current = reservations.findProcess(processId)
                .orElseThrow(() -> new ReservationException(
                        "HOLD_NOT_FOUND", 404, "The reservation process was not found."));
        if (current.status() != ReservationProcessStatus.HELD) {
            return new ReservationConfirmResult(current.reservationProcessId(), current.status().name(),
                    current.expiresAt(), null);
        }
        ReservationProcess waiting = current.waitingForPhone(
                patientFirstName, patientLastName, response.requestPayload(),
                response.expiresAt() == null ? current.expiresAt() : response.expiresAt(), now);
        Reservation existing = reservations.findReservationByProcessId(current.id()).orElse(null);
        Reservation reservation = existing == null
                ? new Reservation(null, current.id(), null, current.ownerPublicId(),
                        current.ownerPublicId().toString(), current.professionalId(), current.appointmentDate(),
                        current.startTime(), current.endTime(), current.professionalFirstName(),
                        current.professionalLastName(), patientFirstName, patientLastName, null,
                        "PHONE_PENDING", now, null, null, null)
                : existing.withEvent(null, "PHONE_PENDING", now);
        reservations.saveConfirmation(waiting, reservation);
        return new ReservationConfirmResult(response.reservationProcessId(),
                "WAITING_FOR_PHONE", waiting.expiresAt(), null);
    }

    private static boolean isInvalidName(String value) {
        return value == null || value.trim().length() < 2 || value.trim().length() > 100;
    }
}
