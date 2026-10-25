package ar.edu.um.turnos.appointment.reservation.domain.ports.out;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationConfirmResult;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationHold;
import java.time.LocalDate;
import java.time.LocalTime;

public interface AppointmentHoldGateway {

    ReservationHold createHold(long professionalId, LocalDate date, LocalTime startTime,
                               String externalPatientId);

    ReservationConfirmResult confirmHold(String holdId, String reservationProcessId, String externalPatientId,
                                          String patientFirstName, String patientLastName);
}
