package ar.edu.um.turnos.appointment.reservation.application.usecases;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessDetails;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.GetReservationProcessUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GetReservationProcessUseCaseImpl implements GetReservationProcessUseCase {

    private final ReservationRepository reservations;

    public GetReservationProcessUseCaseImpl(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public ReservationProcessDetails get(String processId, UUID ownerPublicId) {
        ReservationProcess process = reservations.findProcess(processId)
                .filter(candidate -> ownerPublicId != null && ownerPublicId.equals(candidate.ownerPublicId()))
                .orElseThrow(() -> new ReservationException(
                        "RESERVATION_NOT_FOUND", 404, "The reservation process was not found."));
        return reservations.findReservationByProcessId(process.id())
                .map(reservation -> new ReservationProcessDetails(
                        process.reservationProcessId(), process.status().name(), process.expiresAt(),
                        reservation.reservationId() == null ? process.reservationId() : reservation.reservationId(),
                        process.resultStatus(), process.lastError(), reservation.phoneSubmissionStatus().name()))
                .orElseGet(() -> new ReservationProcessDetails(
                        process.reservationProcessId(), process.status().name(), process.expiresAt(),
                        process.reservationId(), process.resultStatus(), process.lastError(),
                        PhoneSubmissionStatus.NOT_SENT.name()));
    }
}
