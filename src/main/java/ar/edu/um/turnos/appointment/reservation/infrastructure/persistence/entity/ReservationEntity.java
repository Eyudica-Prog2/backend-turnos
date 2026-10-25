package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "reservation", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservation_process", columnNames = "reservation_process_id"),
        @UniqueConstraint(name = "uk_catedra_reservation", columnNames = "catedra_reservation_id")
})
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reservation_process_id", nullable = false, unique = true)
    private Long processId;
    @Column(name = "catedra_reservation_id", unique = true)
    private Long reservationId;
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
    @Column(name = "patient_phone", length = 32)
    private String patientPhone;
    @Column(nullable = false, length = 40)
    private String status;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant confirmedAt;
    private Instant cancelledAt;
    @Column(length = 500)
    private String cancellationReason;

    public ReservationEntity() {
    }

    public Long getId() { return id; }
    public Long getProcessId() { return processId; }
    public Long getReservationId() { return reservationId; }
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
    public String getPatientPhone() { return patientPhone; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setId(Long value) { id = value; }
    public void setProcessId(Long value) { processId = value; }
    public void setReservationId(Long value) { reservationId = value; }
    public void setOwnerPublicId(UUID value) { ownerPublicId = value; }
    public void setExternalPatientId(String value) { externalPatientId = value; }
    public void setProfessionalId(long value) { professionalId = value; }
    public void setAppointmentDate(LocalDate value) { appointmentDate = value; }
    public void setStartTime(LocalTime value) { startTime = value; }
    public void setEndTime(LocalTime value) { endTime = value; }
    public void setProfessionalFirstName(String value) { professionalFirstName = value; }
    public void setProfessionalLastName(String value) { professionalLastName = value; }
    public void setPatientFirstName(String value) { patientFirstName = value; }
    public void setPatientLastName(String value) { patientLastName = value; }
    public void setPatientPhone(String value) { patientPhone = value; }
    public void setStatus(String value) { status = value; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public void setConfirmedAt(Instant value) { confirmedAt = value; }
    public void setCancelledAt(Instant value) { cancelledAt = value; }
    public void setCancellationReason(String value) { cancellationReason = value; }
}
