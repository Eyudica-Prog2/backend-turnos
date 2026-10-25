package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reservation_process")
public class ReservationProcessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reservation_process_id", unique = true, length = 120)
    private String reservationProcessId;
    @Column(name = "hold_id", unique = true, length = 120)
    private String holdId;
    @Column(name = "owner_public_id", nullable = false)
    private UUID ownerPublicId;
    @Column(name = "external_patient_id", nullable = false, length = 100)
    private String externalPatientId;
    @Column(name = "professional_id", nullable = false)
    private long professionalId;
    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(name = "professional_first_name", nullable = false, length = 100)
    private String professionalFirstName;
    @Column(name = "professional_last_name", nullable = false, length = 100)
    private String professionalLastName;
    @Column(name = "patient_first_name", length = 100)
    private String patientFirstName;
    @Column(name = "patient_last_name", length = 100)
    private String patientLastName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReservationProcessStatus status;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "request_event_id", length = 120)
    private String requestEventId;
    @Column(name = "reservation_id", unique = true)
    private Long reservationId;
    @Column(name = "result_status", length = 40)
    private String resultStatus;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "hold_request_payload", columnDefinition = "text")
    private String holdRequestPayload;
    @Column(name = "confirm_request_payload", columnDefinition = "text")
    private String confirmRequestPayload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ReservationProcessEntity() {
    }

    public Long getId() { return id; }
    public String getReservationProcessId() { return reservationProcessId; }
    public String getHoldId() { return holdId; }
    public UUID getOwnerPublicId() { return ownerPublicId; }
    public String getExternalPatientId() { return externalPatientId; }
    public long getProfessionalId() { return professionalId; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getProfessionalFirstName() { return professionalFirstName; }
    public String getProfessionalLastName() { return professionalLastName; }
    public String getPatientFirstName() { return patientFirstName; }
    public String getPatientLastName() { return patientLastName; }
    public ReservationProcessStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getRequestEventId() { return requestEventId; }
    public Long getReservationId() { return reservationId; }
    public String getResultStatus() { return resultStatus; }
    public String getLastError() { return lastError; }
    public String getHoldRequestPayload() { return holdRequestPayload; }
    public String getConfirmRequestPayload() { return confirmRequestPayload; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setReservationProcessId(String value) { this.reservationProcessId = value; }
    public void setHoldId(String value) { this.holdId = value; }
    public void setOwnerPublicId(UUID value) { this.ownerPublicId = value; }
    public void setExternalPatientId(String value) { this.externalPatientId = value; }
    public void setProfessionalId(long value) { this.professionalId = value; }
    public void setAppointmentDate(LocalDate value) { this.appointmentDate = value; }
    public void setStartTime(LocalTime value) { this.startTime = value; }
    public void setEndTime(LocalTime value) { this.endTime = value; }
    public void setProfessionalFirstName(String value) { this.professionalFirstName = value; }
    public void setProfessionalLastName(String value) { this.professionalLastName = value; }
    public void setPatientFirstName(String value) { this.patientFirstName = value; }
    public void setPatientLastName(String value) { this.patientLastName = value; }
    public void setStatus(ReservationProcessStatus value) { this.status = value; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public void setRequestEventId(String value) { this.requestEventId = value; }
    public void setReservationId(Long value) { this.reservationId = value; }
    public void setResultStatus(String value) { this.resultStatus = value; }
    public void setLastError(String value) { this.lastError = value; }
    public void setHoldRequestPayload(String value) { this.holdRequestPayload = value; }
    public void setConfirmRequestPayload(String value) { this.confirmRequestPayload = value; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
