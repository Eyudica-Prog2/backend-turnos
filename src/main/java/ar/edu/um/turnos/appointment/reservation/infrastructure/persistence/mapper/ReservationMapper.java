package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.mapper;

import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationEntity;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public ReservationProcess toDomain(ReservationProcessEntity entity) {
        return new ReservationProcess(entity.getId(), entity.getReservationProcessId(), entity.getHoldId(),
                entity.getOwnerPublicId(), entity.getProfessionalId(), entity.getAppointmentDate(),
                entity.getStartTime(), entity.getEndTime(), entity.getProfessionalFirstName(),
                entity.getProfessionalLastName(), entity.getPatientFirstName(), entity.getPatientLastName(),
                entity.getStatus(), entity.getExpiresAt(), entity.getRequestEventId(),
                entity.getReservationId(), entity.getResultStatus(), entity.getLastError(),
                entity.getHoldRequestPayload(), entity.getConfirmRequestPayload(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public ReservationProcessEntity toEntity(ReservationProcess process) {
        ReservationProcessEntity entity = new ReservationProcessEntity();
        entity.setId(process.id());
        entity.setReservationProcessId(process.reservationProcessId());
        entity.setHoldId(process.holdId());
        entity.setOwnerPublicId(process.ownerPublicId());
        entity.setExternalPatientId(process.ownerPublicId().toString());
        entity.setProfessionalId(process.professionalId());
        entity.setAppointmentDate(process.appointmentDate());
        entity.setStartTime(process.startTime());
        entity.setEndTime(process.endTime());
        entity.setProfessionalFirstName(process.professionalFirstName());
        entity.setProfessionalLastName(process.professionalLastName());
        entity.setPatientFirstName(process.patientFirstName());
        entity.setPatientLastName(process.patientLastName());
        entity.setStatus(process.status());
        entity.setExpiresAt(process.expiresAt());
        entity.setRequestEventId(process.requestEventId());
        entity.setReservationId(process.reservationId());
        entity.setResultStatus(process.resultStatus());
        entity.setLastError(process.lastError());
        entity.setHoldRequestPayload(process.holdRequestPayload());
        entity.setConfirmRequestPayload(process.confirmRequestPayload());
        entity.setCreatedAt(process.createdAt());
        entity.setUpdatedAt(process.updatedAt());
        return entity;
    }

    public Reservation toDomain(ReservationEntity entity) {
        return new Reservation(entity.getId(), entity.getProcessId(), entity.getReservationId(),
                entity.getOwnerPublicId(), entity.getExternalPatientId(), entity.getProfessionalId(),
                entity.getAppointmentDate(), entity.getStartTime(), entity.getEndTime(),
                entity.getProfessionalFirstName(), entity.getProfessionalLastName(),
                entity.getPatientFirstName(), entity.getPatientLastName(), entity.getPatientPhone(),
                entity.getPhoneSubmissionStatus(), entity.getPhoneSubmissionEventId(),
                entity.getStatus(), entity.getCreatedAt(), entity.getConfirmedAt(),
                entity.getCancelledAt(), entity.getCancellationReason());
    }

    public ReservationEntity toEntity(Reservation reservation) {
        ReservationEntity entity = new ReservationEntity();
        entity.setId(reservation.id());
        entity.setProcessId(reservation.processId());
        entity.setReservationId(reservation.reservationId());
        entity.setOwnerPublicId(reservation.ownerPublicId());
        entity.setExternalPatientId(reservation.externalPatientId());
        entity.setProfessionalId(reservation.professionalId());
        entity.setAppointmentDate(reservation.appointmentDate());
        entity.setStartTime(reservation.startTime());
        entity.setEndTime(reservation.endTime());
        entity.setProfessionalFirstName(reservation.professionalFirstName());
        entity.setProfessionalLastName(reservation.professionalLastName());
        entity.setPatientFirstName(reservation.patientFirstName());
        entity.setPatientLastName(reservation.patientLastName());
        entity.setPatientPhone(reservation.patientPhone());
        entity.setPhoneSubmissionStatus(reservation.phoneSubmissionStatus());
        entity.setPhoneSubmissionEventId(reservation.phoneSubmissionEventId());
        entity.setStatus(reservation.status());
        entity.setCreatedAt(reservation.createdAt());
        entity.setConfirmedAt(reservation.confirmedAt());
        entity.setCancelledAt(reservation.cancelledAt());
        entity.setCancellationReason(reservation.cancellationReason());
        return entity;
    }
}
