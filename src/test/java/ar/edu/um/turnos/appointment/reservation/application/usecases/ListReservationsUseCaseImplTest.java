package ar.edu.um.turnos.appointment.reservation.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.appointment.reservation.application.exception.ReservationException;
import ar.edu.um.turnos.appointment.reservation.domain.model.AppointmentListingPage;
import ar.edu.um.turnos.appointment.reservation.domain.model.PhoneSubmissionStatus;
import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationQuery;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.AppointmentListingGateway;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListReservationsUseCaseImplTest {

    private static final UUID OWNER = UUID.fromString("a43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final UUID OTHER_OWNER = UUID.fromString("b43d7a0b-fec6-4f3a-98c6-d2e283194bd9");
    private static final Instant CREATED_AT = Instant.parse("2026-10-20T10:00:00Z");

    @Mock private AppointmentListingGateway appointmentListing;
    @Mock private ReservationRepository reservations;
    @InjectMocks private ListReservationsUseCaseImpl useCase;

    @Test
    void synchronizesOnlyAppointmentsMatchingTheAuthenticatedPatientAndPreservesPageMetadata() {
        Reservation owned = appointment(8251L, "process-1", OWNER, OWNER.toString());
        Reservation leaked = appointment(8252L, "process-2", OWNER, OTHER_OWNER.toString());
        ReservationQuery query = new ReservationQuery("CONFIRMED", LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-31"), 2, 10);
        when(appointmentListing.list(OWNER, query))
                .thenReturn(new AppointmentListingPage(List.of(owned, leaked), 21, "<next>; rel=\"next\""));
        when(reservations.findReservationByReservationId(8251L)).thenReturn(Optional.empty());
        when(reservations.saveReservation(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var page = useCase.list(OWNER, query);

        assertThat(page.reservations()).containsExactly(owned);
        assertThat(page.totalCount()).isEqualTo(21);
        assertThat(page.link()).isEqualTo("<next>; rel=\"next\"");
        verify(appointmentListing).list(OWNER, query);
        verify(reservations).saveReservation(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.ownerPublicId().equals(OWNER) && saved.reservationId().equals(8251L)));
        verify(reservations, never()).saveReservation(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.reservationId().equals(8252L)));
    }

    @Test
    void doesNotOverwriteAReservationAlreadyOwnedByAnotherUser() {
        Reservation remote = appointment(8251L, "process-1", OWNER, OWNER.toString());
        Reservation existing = appointment(8251L, "process-1", OTHER_OWNER, OTHER_OWNER.toString());
        ReservationQuery query = new ReservationQuery(null, null, null, 0, 20);
        when(appointmentListing.list(OWNER, query))
                .thenReturn(new AppointmentListingPage(List.of(remote), 1, null));
        when(reservations.findReservationByReservationId(8251L)).thenReturn(Optional.of(existing));

        var page = useCase.list(OWNER, query);

        assertThat(page.reservations()).isEmpty();
        verify(reservations, never()).saveReservation(any());
    }

    @Test
    void rejectsInvalidDateRangeOrPaginationBeforeCallingCatedra() {
        assertThatThrownBy(() -> useCase.list(OWNER, new ReservationQuery(null,
                LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-01"), 0, 20)))
                .isInstanceOf(ReservationException.class);
        assertThatThrownBy(() -> useCase.list(OWNER, new ReservationQuery(null, null, null, 0, 101)))
                .isInstanceOf(ReservationException.class);
        verify(appointmentListing, never()).list(any(), any());
    }

    private static Reservation appointment(Long reservationId, String processId, UUID owner, String externalId) {
        return new Reservation(null, null, reservationId, processId, owner, externalId, 15,
                LocalDate.parse("2026-10-26"), LocalTime.parse("09:00"), LocalTime.parse("09:30"),
                "Ana", "Gomez", "Juan", "Perez", null, PhoneSubmissionStatus.NOT_SENT, null,
                "CONFIRMED", CREATED_AT, CREATED_AT.plusSeconds(60), null, null);
    }
}
