# Backend de turnos

La arquitectura combinada de los dos backends, la propiedad de datos, el contrato HTTP con
`backend-catalogo`, la decisión de JWT y el transporte de despliegue están documentados en
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

Servicio de turnos y reservas del proyecto. Incluye PostgreSQL propio, validación del JWT de usuario, consulta de disponibilidad, creación y confirmación inicial de holds, y consumo idempotente de eventos de reserva.

## Requisitos

- JDK 21
- Docker con Docker Compose

## Configuración y ejecución local

1. Crear la configuración local: `cp .env.example .env`.
2. Revisar `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT`, `APP_TIME_ZONE` y los puertos HTTP. `APP_TIME_ZONE` debe coincidir con la zona horaria de los horarios de atención. `.env` está excluido de Git; usar valores reales solo localmente.
3. Crear una sola vez la red común (si aún no existe): `docker network create backend-integration`.
4. Desde esta carpeta, ejecutar `docker compose up --build`.

Compose levanta únicamente `backend-turnos` y su PostgreSQL 16. La base de datos y el usuario son propios de este servicio, independientes de `backend-catalogo`.
Al compartir `backend-integration`, el contenedor usa el DNS `backend-catalogo:8081` para su
contrato HTTP; al ejecutar desde el host, el `.env` conserva `http://localhost:8081`.

Para ejecutar el servicio desde el host, con PostgreSQL levantado: `./mvnw spring-boot:run`.

## Pruebas

Ejecutar `./mvnw test`. La prueba de contexto usa PostgreSQL con Testcontainers; requiere Docker disponible. No se utiliza H2.

## Contrato de disponibilidad

`GET /api/professionals/{id}/availability?date=yyyy-MM-dd` devuelve `{professionalId, date, slots}`. Cada slot contiene `startTime` y `endTime`. La lista incluye únicamente slots futuros y libres; los ocupados se omiten, por lo que no se agrega un campo `available` redundante.

## Inicio y confirmación de reservas

- `POST /api/reservations` acepta únicamente `professionalId`, `date` y `startTime`; el `externalPatientId` se deriva del claim `publicId` del JWT de usuario.
- `POST /api/reservations/{reservationProcessId}/confirm` verifica propiedad y vencimiento antes de confirmar el hold.
- `POST /api/reservations/{reservationProcessId}/phone` acepta `{ "phoneNumber": "..." }`, normaliza espacios, guiones, paréntesis y puntos, y publica `AdditionalInformationSubmitted` en el topic asignado. Solo responde `202` después del ACK de Kafka.
- `GET /api/reservations/{reservationProcessId}` devuelve el estado, vencimiento, resultado/error y estado de envío del teléfono; solo el usuario dueño puede consultarlo.
- El consumidor Kafka obtiene bootstrap server, consumer group y topic desde la configuración de integración de cátedra. Si esa configuración no está disponible al inicio, se vuelve a consultar periódicamente. Si un error de procesamiento detiene el contenedor, el backend lo recrea en el siguiente ciclo de recuperación. Deduplica `eventId` y confirma el offset solo luego de persistir el cambio; el registro fallido permanece sin confirmar para poder reprocesarse.
- Para confirmar, el JWT debe incluir además los claims `firstName` y `lastName`, emitidos por `backend-catalogo`. Tokens anteriores que no los incluyan no pueden confirmar un hold y reciben `USER_PROFILE_CLAIMS_MISSING`; basta volver a autenticarse para recibir un token actualizado.
- Una respuesta ambigua al confirmar se reconcilia por `GET /api/appointments` antes de considerar un reintento. La creación/confirmación no se reintenta ciegamente.
- Un envío de teléfono sin ACK queda como `PENDING`: no se reenvía automáticamente. El proceso se puede consultar y la recepción de un rechazo habilita un nuevo intento con un `eventId` nuevo.
- El teléfono se valida después de normalizar con `^\+?[0-9]{7,15}$`; no se incluye en respuestas ni logs.

## Consulta de reservas propias

`GET /api/reservations?status=CONFIRMED&from=2026-10-01&to=2026-10-31&page=0&size=20` consulta la cuenta técnica de cátedra y sincroniza la página solicitada en la base local. El paciente se obtiene exclusivamente del claim `publicId` del JWT; no se acepta `externalPatientId` como filtro del cliente. La respuesta solo incluye reservas asociadas a ese usuario, propaga `X-Total-Count` y entrega enlaces `Link` apuntando a este backend. El teléfono del paciente no se expone en la respuesta.

`POST /api/reservations/{reservationProcessId}/cancel` cancela una reserva confirmada del usuario autenticado. El body es opcional (`{"reason":"..."}`; hasta 500 caracteres). Las reservas inexistentes y ajenas responden igual (`404 RESERVATION_NOT_FOUND`); una reserva ya cancelada devuelve su estado sin repetir la llamada a cátedra.

## Recuperación de procesos

Los procesos `HELD`, `WAITING_FOR_PHONE` y `REJECTED_RETRYABLE` se conservan en PostgreSQL y se revisan al iniciar y periódicamente. Si `expiresAt` ya pasó, el backend consulta primero los appointments de cátedra para esa fecha y ese usuario: sincroniza un estado remoto encontrado o cierra localmente como `EXPIRED`/`FAILED` si no existe. No repite operaciones REST con efectos. Las fallas remotas mantienen el proceso pendiente, guardan un código estable y permiten reintentar con backoff.

La tarea se configura con `APP_RESERVATION_RECONCILIATION_ENABLED`, `APP_RESERVATION_RECONCILIATION_INITIAL_DELAY_MS`, `APP_RESERVATION_RECONCILIATION_FIXED_DELAY_MS`, `APP_RESERVATION_RECONCILIATION_RETRY_BACKOFF_MS`, `APP_RESERVATION_RECONCILIATION_BATCH_SIZE`, `APP_RESERVATION_RECONCILIATION_PAGE_SIZE` y `APP_RESERVATION_RECONCILIATION_MAX_PAGES`. CORS se controla con `CORS_ALLOWED_ORIGINS` (lista separada por comas; vacío por defecto).

La recuperación del listener se configura con `APP_KAFKA_LISTENER_RECOVERY_INITIAL_DELAY_MS` y
`APP_KAFKA_LISTENER_RECOVERY_FIXED_DELAY_MS` (30 segundos por defecto). Los Compose locales
limitan puertos a loopback; producción debe terminar TLS en un ingress privado y no exponer
directamente los puertos HTTP del backend.
