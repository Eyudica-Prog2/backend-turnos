package ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.adapter;

import ar.edu.um.turnos.appointment.reservation.domain.model.Reservation;
import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationProcess;
import ar.edu.um.turnos.appointment.reservation.domain.ports.out.ReservationRepository;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.mapper.ReservationMapper;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository.JpaProcessedEventRepository;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository.JpaReservationProcessRepository;
import ar.edu.um.turnos.appointment.reservation.infrastructure.persistence.repository.JpaReservationRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaReservationRepositoryAdapter implements ReservationRepository {

    private final JpaReservationProcessRepository processRepository;
    private final JpaReservationRepository reservationRepository;
    private final JpaProcessedEventRepository eventRepository;
    private final ReservationMapper mapper;

    public JpaReservationRepositoryAdapter(JpaReservationProcessRepository processRepository,
                                          JpaReservationRepository reservationRepository,
                                          JpaProcessedEventRepository eventRepository,
                                          ReservationMapper mapper) {
        this.processRepository = processRepository;
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ReservationProcess saveProcess(ReservationProcess process) {
        return mapper.toDomain(processRepository.save(mapper.toEntity(process)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReservationProcess> findProcess(String reservationProcessId) {
        return processRepository.findByReservationProcessId(reservationProcessId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteProcess(Long id) {
        if (id != null) {
            processRepository.deleteById(id);
        }
    }

    @Override
    @Transactional
    public Reservation saveReservation(Reservation reservation) {
        return mapper.toDomain(reservationRepository.save(mapper.toEntity(reservation)));
    }

    @Override
    @Transactional
    public void saveConfirmation(ReservationProcess process, Reservation reservation) {
        processRepository.save(mapper.toEntity(process));
        reservationRepository.save(mapper.toEntity(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reservation> findReservationByProcessId(Long processId) {
        return reservationRepository.findByProcessId(processId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public boolean registerEventIfNew(String eventId, String eventType, Instant processedAt) {
        return eventRepository.insertIfAbsent(eventId, eventType, processedAt) == 1;
    }
}
