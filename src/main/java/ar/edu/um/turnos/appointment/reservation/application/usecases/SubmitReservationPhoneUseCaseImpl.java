package ar.edu.um.turnos.appointment.reservation.application.usecases;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmission;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionConfiguration;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.SubmitReservationPhoneUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.PhoneSubmissionConfigurationGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.PhoneSubmissionGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SubmitReservationPhoneUseCaseImpl implements SubmitReservationPhoneUseCase {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");

    private final ReservationRepository reservations;
    private final PhoneSubmissionConfigurationGateway configurationGateway;
    private final PhoneSubmissionGateway phoneSubmissionGateway;
    private final Clock clock;

    public SubmitReservationPhoneUseCaseImpl(ReservationRepository reservations,
                                            PhoneSubmissionConfigurationGateway configurationGateway,
                                            PhoneSubmissionGateway phoneSubmissionGateway,
                                            Clock clock) {
        this.reservations = reservations;
        this.configurationGateway = configurationGateway;
        this.phoneSubmissionGateway = phoneSubmissionGateway;
        this.clock = clock;
    }

    @Override
    public PhoneSubmissionResult submit(String processId, UUID ownerPublicId, String rawPhone) {
        ReservationProcess process = findOwnedProcess(processId, ownerPublicId);
        if (process.status() != ReservationProcessStatus.WAITING_FOR_PHONE
                && process.status() != ReservationProcessStatus.REJECTED_RETRYABLE) {
            throw new ReservationException("RESERVATION_PROCESS_NOT_WAITING_FOR_PHONE", 409,
                    "The reservation process is not accepting a phone number.");
        }
        if (process.expiresAt() == null || !process.expiresAt().isAfter(clock.instant())) {
            throw new ReservationException("RESERVATION_PROCESS_EXPIRED", 409,
                    "The reservation process has expired.");
        }
        if (blank(process.holdId()) || blank(process.requestEventId())) {
            throw new ReservationException("PHONE_REQUEST_NOT_AVAILABLE", 409,
                    "The catedra has not requested phone information for this process.");
        }

        String phoneNumber = normalizeAndValidate(rawPhone);
        Reservation reservation = reservations.findReservationByProcessId(process.id())
                .orElseThrow(() -> new ReservationException(
                        "RESERVATION_NOT_FOUND", 404, "The reservation process was not found."));
        ensureCanSubmit(reservation.phoneSubmissionStatus());

        // Resolve external configuration before claiming this attempt in the database.
        PhoneSubmissionConfiguration configuration = configurationGateway.currentConfiguration();
        String eventId = UUID.randomUUID().toString();
        Reservation pendingReservation = reservation.phoneSubmissionPending(phoneNumber, eventId);
        if (!reservations.beginPhoneSubmission(pendingReservation, process.requestEventId(), clock.instant())) {
            ReservationProcess currentProcess = reservations.findProcess(processId).orElse(process);
            ensureProcessAcceptsPhone(currentProcess);
            if (!Objects.equals(process.requestEventId(), currentProcess.requestEventId())) {
                throw new ReservationException("PHONE_REQUEST_CHANGED", 409,
                        "A new phone request was received; query the process before submitting again.");
            }
            PhoneSubmissionStatus latestStatus = reservations.findReservationByProcessId(process.id())
                    .map(Reservation::phoneSubmissionStatus)
                    .orElse(PhoneSubmissionStatus.PENDING);
            ensureCanSubmit(latestStatus);
            throw new ReservationException("PHONE_SUBMISSION_PENDING", 409,
                    "A phone submission is already being processed.");
        }

        PhoneSubmission submission = new PhoneSubmission(eventId, process.reservationProcessId(),
                process.holdId(), process.requestEventId(), phoneNumber, clock.instant());
        try {
            phoneSubmissionGateway.publish(submission, configuration);
        } catch (RuntimeException publicationFailure) {
            reservations.recordProcessError(process.id(),
                    "The phone submission result is uncertain; do not resend it blindly.", clock.instant());
            throw new ReservationException("PHONE_SUBMISSION_UNCERTAIN", 503,
                    "The phone submission could not be confirmed. Check the reservation status before retrying.");
        }

        boolean markedSent = reservations.markPhoneSubmissionSent(pendingReservation.phoneSubmissionSent(eventId));
        if (!markedSent) {
            ReservationProcess latestProcess = reservations.findProcess(processId).orElse(process);
            PhoneSubmissionStatus latestSubmissionStatus = reservations.findReservationByProcessId(process.id())
                    .map(Reservation::phoneSubmissionStatus).orElse(PhoneSubmissionStatus.PENDING);
            return new PhoneSubmissionResult(latestProcess.reservationProcessId(),
                    latestProcess.status().name(), latestSubmissionStatus.name(), latestProcess.expiresAt());
        }
        return new PhoneSubmissionResult(process.reservationProcessId(), process.status().name(),
                PhoneSubmissionStatus.SENT.name(), process.expiresAt());
    }

    private ReservationProcess findOwnedProcess(String processId, UUID ownerPublicId) {
        if (blank(processId) || ownerPublicId == null) {
            throw new ReservationException("RESERVATION_NOT_FOUND", 404, "The reservation process was not found.");
        }
        return reservations.findProcess(processId)
                .filter(process -> ownerPublicId.equals(process.ownerPublicId()))
                .orElseThrow(() -> new ReservationException(
                        "RESERVATION_NOT_FOUND", 404, "The reservation process was not found."));
    }

    private static String normalizeAndValidate(String rawPhone) {
        if (rawPhone == null) {
            throw invalidPhone();
        }
        StringBuilder normalized = new StringBuilder(rawPhone.length());
        for (int index = 0; index < rawPhone.length(); index++) {
            char character = rawPhone.charAt(index);
            if (Character.isWhitespace(character) || character == '-' || character == '(' 
                    || character == ')' || character == '.') {
                continue;
            }
            normalized.append(character);
        }
        String phoneNumber = normalized.toString();
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw invalidPhone();
        }
        return phoneNumber;
    }

    private static void ensureCanSubmit(PhoneSubmissionStatus status) {
        if (status == PhoneSubmissionStatus.PENDING) {
            throw new ReservationException("PHONE_SUBMISSION_PENDING", 409,
                    "A phone submission is already being processed; do not send it again.");
        }
        if (status == PhoneSubmissionStatus.SENT) {
            throw new ReservationException("PHONE_ALREADY_SUBMITTED", 409,
                    "A phone number was already submitted for the current request.");
        }
    }

    private void ensureProcessAcceptsPhone(ReservationProcess process) {
        if (process.expiresAt() == null || !process.expiresAt().isAfter(clock.instant())) {
            throw new ReservationException("RESERVATION_PROCESS_EXPIRED", 409,
                    "The reservation process has expired.");
        }
        if (process.status() != ReservationProcessStatus.WAITING_FOR_PHONE
                && process.status() != ReservationProcessStatus.REJECTED_RETRYABLE) {
            throw new ReservationException("RESERVATION_PROCESS_NOT_WAITING_FOR_PHONE", 409,
                    "The reservation process is not accepting a phone number.");
        }
        if (blank(process.requestEventId())) {
            throw new ReservationException("PHONE_REQUEST_NOT_AVAILABLE", 409,
                    "The catedra has not requested phone information for this process.");
        }
    }

    private static ReservationException invalidPhone() {
        return new ReservationException("INVALID_PHONE_NUMBER", 400,
                "The phone number must contain 7 to 15 digits and an optional leading plus sign.");
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
