package ar.edu.um.turnos.appointment.reservation.infrastructure.scheduling;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import ar.edu.um.turnos.appointment.reservation.domain.model.ReservationRecoveryPolicy;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ReservationReconciliationProperties.class)
public class ReservationReconciliationConfiguration {

    @Bean
    ReservationRecoveryPolicy reservationRecoveryPolicy(ReservationReconciliationProperties properties) {
        return new ReservationRecoveryPolicy(Duration.ofMillis(properties.retryBackoffMs()),
                properties.batchSize(), properties.pageSize(), properties.maxPages());
    }
}
