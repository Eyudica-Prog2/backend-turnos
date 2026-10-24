package ar.edu.um.turnos.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class TurnosApplicationTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USER", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);
        registry.add("APP_JWT_SECRET", () -> "test-only-app-jwt-secret-with-minimum-length");
        registry.add("APP_TIME_ZONE", () -> "UTC");
        registry.add("CATEDRA_API_BASE_URL", () -> "http://localhost:9999");
        registry.add("CATEDRA_HTTP_CONNECT_TIMEOUT_MS", () -> "1000");
        registry.add("CATEDRA_HTTP_READ_TIMEOUT_MS", () -> "1000");
        registry.add("CATEDRA_RETRY_MAX_ATTEMPTS", () -> "1");
        registry.add("CATEDRA_RETRY_INITIAL_BACKOFF_MS", () -> "0");
        registry.add("CATEDRA_RETRY_MAX_BACKOFF_MS", () -> "0");
        registry.add("CATALOGO_API_BASE_URL", () -> "http://localhost:8081");
        registry.add("CATALOGO_HTTP_CONNECT_TIMEOUT_MS", () -> "1000");
        registry.add("CATALOGO_HTTP_READ_TIMEOUT_MS", () -> "1000");
    }

    @Test
    void applicationContextStartsWithPostgres() {
        // Starting the Spring context verifies datasource and Flyway configuration.
    }
}
