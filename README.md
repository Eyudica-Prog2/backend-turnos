# Backend de turnos

Servicio de turnos y reservas del proyecto. Incluye PostgreSQL propio, validación del JWT de usuario, consulta de disponibilidad, creación y confirmación inicial de holds, y consumo idempotente de eventos de reserva.

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

## Inicio y confirmación de reservas

- `POST /api/reservations` acepta únicamente `professionalId`, `date` y `startTime`; el `externalPatientId` se deriva del claim `publicId` del JWT de usuario.
- `POST /api/reservations/{reservationProcessId}/confirm` verifica propiedad y vencimiento antes de confirmar el hold.
- `POST /api/reservations/{reservationProcessId}/phone` acepta `{ "phoneNumber": "..." }`, normaliza espacios, guiones, paréntesis y puntos, y publica `AdditionalInformationSubmitted` en el topic asignado. Solo responde `202` después del ACK de Kafka.
- `GET /api/reservations/{reservationProcessId}` devuelve el estado, vencimiento, resultado/error y estado de envío del teléfono; solo el usuario dueño puede consultarlo.
- El consumidor Kafka obtiene bootstrap server, consumer group y topic desde la configuración de integración de cátedra al iniciar. Deduplica `eventId` y confirma el offset solo luego de persistir el cambio.
- Para confirmar, el JWT debe incluir además los claims `firstName` y `lastName`, emitidos por `backend-catalogo`. Tokens anteriores que no los incluyan no pueden confirmar un hold y reciben `USER_PROFILE_CLAIMS_MISSING`; basta volver a autenticarse para recibir un token actualizado.
- Una respuesta ambigua al confirmar se reconcilia por `GET /api/appointments` antes de considerar un reintento. La creación/confirmación no se reintenta ciegamente.
- Un envío de teléfono sin ACK queda como `PENDING`: no se reenvía automáticamente. El proceso se puede consultar y la recepción de un rechazo habilita un nuevo intento con un `eventId` nuevo.
- El teléfono se valida después de normalizar con `^\+?[0-9]{7,15}$`; no se incluye en respuestas ni logs.

## Consulta de reservas propias

`GET /api/reservations?status=CONFIRMED&from=2026-10-01&to=2026-10-31&page=0&size=20` consulta la cuenta técnica de cátedra y sincroniza la página solicitada en la base local. El paciente se obtiene exclusivamente del claim `publicId` del JWT; no se acepta `externalPatientId` como filtro del cliente. La respuesta solo incluye reservas asociadas a ese usuario, propaga `X-Total-Count` y entrega enlaces `Link` apuntando a este backend. El teléfono del paciente no se expone en la respuesta.
