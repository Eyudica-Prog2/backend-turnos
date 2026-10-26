package ar.edu.um.turnos.appointment.reservation.application.usecases;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.CreateReservationUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentHoldGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.shared.domain.model.AppointmentSlotAvailability;
import ar.edu.um.turnos.appointment.shared.domain.model.SlotOccupancy;
import ar.edu.um.turnos.appointment.shared.domain.ports.out.AppointmentSlotAvailabilityGateway;
import ar.edu.um.turnos.appointment.shared.error.CatedraClientException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CreateReservationUseCaseImpl implements CreateReservationUseCase {

    private final AppointmentSlotAvailabilityGateway slotAvailability;
    private final AppointmentHoldGateway catedra;
    private final ReservationRepository reservations;
    private final Clock clock;

    public CreateReservationUseCaseImpl(AppointmentSlotAvailabilityGateway slotAvailability,
                                        AppointmentHoldGateway catedra,
                                        ReservationRepository reservations,
                                        Clock clock) {
        this.slotAvailability = slotAvailability;
        this.catedra = catedra;
        this.reservations = reservations;
        this.clock = clock;
    }

    @Override
    public ReservationHold create(UUID ownerPublicId, long professionalId, LocalDate date, LocalTime startTime) {
        if (ownerPublicId == null || professionalId <= 0 || date == null || startTime == null) {
            throw new ReservationException("VALIDATION_ERROR", 400, "A valid owner and appointment slot are required.");
        }

        if (date.isBefore(LocalDate.now(clock))
                || (date.equals(LocalDate.now(clock)) && !startTime.isAfter(LocalTime.now(clock)))) {
            throw invalidSlot();
        }

        AppointmentSlotAvailability slot = slotAvailability.inspect(professionalId, date, startTime);
        if (!slot.professionalEnabled()) {
            throw new ReservationException("PROFESSIONAL_DISABLED", 400, "The selected professional is disabled.");
        }
        if (slot.endTime() == null) {
            throw invalidSlot();
        }
        if (slot.occupancy() == SlotOccupancy.CONFIRMED) {
            throw new ReservationException("SLOT_ALREADY_RESERVED", 409, "The selected slot is already reserved.");
        }
        if (slot.occupancy() == SlotOccupancy.HELD && slot.occupiedUntil() != null
                && slot.occupiedUntil().isAfter(clock.instant())) {
            throw new ReservationException("SLOT_ALREADY_HELD", 409, "The selected slot is already held.");
        }

        Instant now = clock.instant();
        ReservationProcess process = ReservationProcess.created(ownerPublicId, professionalId, date,
                startTime, slot.endTime(), slot.professionalFirstName(), slot.professionalLastName(),
                holdRequestPayload(ownerPublicId, professionalId, date, startTime), now);
        process = reservations.saveProcess(process);

        try {
            ReservationHold hold = catedra.createHold(professionalId, date, startTime, ownerPublicId.toString());
            ReservationProcess held = process.held(
                    hold.reservationProcessId(), hold.holdId(), hold.expiresAt(),
                    hold.requestPayload(), clock.instant());
            reservations.saveProcess(held);
            return hold;
        } catch (CatedraClientException failure) {
            // A timeout can mean that catedra created the hold. Keep the CREATED record for later reconciliation;
            // definitive client rejections did not create a remote effect and should not leave a local process.
            if (failure.getHttpStatus() < 500) {
                reservations.deleteProcess(process.id());
            } else {
                reservations.recordProcessError(process.id(), failure.getMessage(), clock.instant());
            }
            throw failure;
        }
    }

    private static ReservationException invalidSlot() {
        return new ReservationException("INVALID_SLOT", 400, "The selected time is not an available slot.");
    }

    private static String holdRequestPayload(UUID ownerPublicId, long professionalId,
                                             LocalDate date, LocalTime startTime) {
        return "{\"professionalId\":" + professionalId
                + ",\"date\":\"" + date + "\""
                + ",\"startTime\":\"" + DateTimeFormatter.ofPattern("HH:mm").format(startTime) + "\""
                + ",\"externalPatientId\":\"" + ownerPublicId + "\"}";
    }
}
