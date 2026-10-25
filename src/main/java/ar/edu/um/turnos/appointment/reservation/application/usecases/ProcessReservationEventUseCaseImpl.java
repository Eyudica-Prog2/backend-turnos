package ar.edu.um.turnos.appointment.reservation.application.usecases;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationEvent;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ProcessReservationEventUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProcessReservationEventUseCaseImpl implements ProcessReservationEventUseCase {

    private static final Set<String> SUPPORTED_EVENTS = Set.of(
            "AdditionalInformationRequested", "AppointmentConfirmed", "AdditionalInformationRejected",
            "AppointmentProcessExpired", "AppointmentProcessInvalid");

    private final ReservationRepository reservations;
    private final Clock clock;

    public ProcessReservationEventUseCaseImpl(ReservationRepository reservations, Clock clock) {
        this.reservations = reservations;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void process(ReservationEvent event) {
        validate(event);
        if (!reservations.registerEventIfNew(event.eventId(), event.eventType(), clock.instant())) {
            return;
        }

        ReservationProcess process = reservations.findProcess(event.reservationProcessId())
                .orElseThrow(() -> new ReservationException(
                        "RESERVATION_PROCESS_NOT_FOUND", 404,
                        "The event references an unknown reservation process."));
        if (process.holdId() != null && event.holdId() != null
                && !process.holdId().equals(event.holdId())) {
            throw new ReservationException("RESERVATION_EVENT_MISMATCH", 409,
                    "The event hold identifier does not match the stored process.");
        }
        if (event.requestEventId() != null && process.requestEventId() != null
                && !process.requestEventId().equals(event.requestEventId())) {
            return;
        }
        ReservationProcess updated = process.apply(event, clock.instant());
        if (updated == process) {
            return;
        }
        reservations.saveProcess(updated);

        Reservation reservation = reservations.findReservationByProcessId(process.id())
                .orElseGet(() -> initialReservation(process, event));
        String status = switch (event.eventType()) {
            case "AppointmentConfirmed" -> "CONFIRMED";
            case "AppointmentProcessExpired", "AppointmentProcessInvalid" -> "FAILED";
            default -> "PHONE_PENDING";
        };
        reservations.saveReservation(reservation.withEvent(event, status, clock.instant()));
    }

    private static Reservation initialReservation(ReservationProcess process, ReservationEvent event) {
        LocalDate date = event.appointmentDate() == null
                ? process.appointmentDate() : LocalDate.parse(event.appointmentDate());
        LocalTime startTime = event.startTime() == null
                ? process.startTime() : LocalTime.parse(event.startTime());
        return new Reservation(null, process.id(), event.reservationId(), process.ownerPublicId(),
                process.ownerPublicId().toString(),
                event.professionalId() == null ? process.professionalId() : event.professionalId(),
                date, startTime, process.endTime(), process.professionalFirstName(),
                process.professionalLastName(), process.patientFirstName(), process.patientLastName(),
                null, "PHONE_PENDING", process.createdAt(), null, null, null);
    }

    private static void validate(ReservationEvent event) {
        if (event == null || blank(event.eventId()) || blank(event.eventType())
                || blank(event.reservationProcessId()) || event.schemaVersion() == null
                || event.schemaVersion() != 1 || !SUPPORTED_EVENTS.contains(event.eventType())) {
            throw new IllegalArgumentException("The reservation event does not match schema version 1.");
        }
        switch (event.eventType()) {
            case "AdditionalInformationRequested" -> {
                if (blank(event.holdId()) || event.expiresAt() == null) {
                    throw new IllegalArgumentException("AdditionalInformationRequested is incomplete.");
                }
            }
            case "AppointmentConfirmed" -> {
                if (event.reservationId() == null || event.professionalId() == null
                        || blank(event.appointmentDate()) || blank(event.startTime())
                        || !"CONFIRMED".equals(event.status())) {
                    throw new IllegalArgumentException("AppointmentConfirmed is incomplete.");
                }
            }
            case "AdditionalInformationRejected" -> {
                if (blank(event.reason()) || event.expiresAt() == null) {
                    throw new IllegalArgumentException("AdditionalInformationRejected is incomplete.");
                }
            }
            case "AppointmentProcessExpired", "AppointmentProcessInvalid" -> {
                if (blank(event.reason())) {
                    throw new IllegalArgumentException("The terminal reservation event is incomplete.");
                }
            }
            default -> throw new IllegalArgumentException("Unsupported reservation event type.");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
