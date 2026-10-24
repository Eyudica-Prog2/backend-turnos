# Backend de turnos

Servicio de turnos y reservas del proyecto. Incluye el esqueleto Spring Boot, PostgreSQL propio, validación de JWT, clientes de integración y el endpoint de consulta de disponibilidad. El flujo de reserva se incorporará en las siguientes iteraciones.

## Requisitos

- JDK 21
- Docker con Docker Compose

## Configuración y ejecución local

1. Crear la configuración local: `cp .env.example .env`.
2. Revisar `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT`, `APP_TIME_ZONE` y los puertos HTTP. `APP_TIME_ZONE` debe coincidir con la zona horaria de los horarios de atención. `.env` está excluido de Git; usar valores reales solo localmente.
3. Desde esta carpeta, ejecutar `docker compose up --build`.

Compose levanta únicamente `backend-turnos` y su PostgreSQL 16. La base de datos y el usuario son propios de este servicio, independientes de `backend-catalogo`.

Para ejecutar el servicio desde el host, con PostgreSQL levantado: `./mvnw spring-boot:run`.

## Pruebas

Ejecutar `./mvnw test`. La prueba de contexto usa PostgreSQL con Testcontainers; requiere Docker disponible. No se utiliza H2.

## Contrato de disponibilidad

`GET /api/professionals/{id}/availability?date=yyyy-MM-dd` devuelve `{professionalId, date, slots}`. Cada slot contiene `startTime` y `endTime`. La lista incluye únicamente slots futuros y libres; los ocupados se omiten, por lo que no se agrega un campo `available` redundante.
