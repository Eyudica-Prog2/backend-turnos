package ar.edu.um.turnos.appointment.reservation.infrastructure.web.controller;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.ConfirmReservationUseCase;
import ar.edu.um.turnos.appointment.reservation.domain.ports.in.CreateReservationUseCase;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto.CreateReservationRequest;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.dto.ReservationHoldResponse;
import ar.edu.um.turnos.appointment.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import ar.edu.um.turnos.appointment.shared.security.AuthenticatedUser;
import ar.edu.um.turnos.appointment.shared.security.AuthenticatedUserProvider;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final CreateReservationUseCase createReservation;
    private final ConfirmReservationUseCase confirmReservation;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final ReservationDtoMapper mapper;

    public ReservationController(CreateReservationUseCase createReservation,
                                 ConfirmReservationUseCase confirmReservation,
                                 AuthenticatedUserProvider authenticatedUserProvider,
                                 ReservationDtoMapper mapper) {
        this.createReservation = createReservation;
        this.confirmReservation = confirmReservation;
        this.authenticatedUserProvider = authenticatedUserProvider;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ReservationHoldResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        AuthenticatedUser user = authenticatedUserProvider.currentUser();
        ReservationHold hold = createReservation.create(
                user.publicId(), request.professionalId(), request.date(), request.startTime());
        ReservationHoldResponse response = mapper.toResponse(hold);
        return ResponseEntity.created(URI.create("/api/reservations/" + hold.reservationProcessId()))
                .body(response);
    }

    @PostMapping("/{reservationProcessId}/confirm")
    public ResponseEntity<ReservationConfirmResult> confirm(
            @PathVariable String reservationProcessId) {
        AuthenticatedUser user = authenticatedUserProvider.currentUser();
        ReservationConfirmResult response = confirmReservation.confirm(
                reservationProcessId, user.publicId(), user.firstName(), user.lastName());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ReservationConfirmResult(response.reservationProcessId(),
                        response.status(), response.expiresAt(), null));
    }
}
