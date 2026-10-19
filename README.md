# Backend de turnos

Servicio de turnos y reservas del proyecto. En esta primera etapa incluye el esqueleto Spring Boot, PostgreSQL propio y migraciones Flyway; las APIs se incorporan en las siguientes tareas.

## Requisitos

- JDK 21
- Docker con Docker Compose

## Configuración y ejecución local

1. Crear la configuración local: `cp .env.example .env`.
2. Revisar `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_PORT` y los puertos HTTP. `.env` está excluido de Git; usar valores reales solo localmente.
3. Desde esta carpeta, ejecutar `docker compose up --build`.

Compose levanta únicamente `backend-turnos` y su PostgreSQL 16. La base de datos y el usuario son propios de este servicio, independientes de `backend-catalogo`.

Para ejecutar el servicio desde el host, con PostgreSQL levantado: `./mvnw spring-boot:run`.

## Pruebas

Ejecutar `./mvnw test`. La prueba de contexto usa PostgreSQL con Testcontainers; requiere Docker disponible. No se utiliza H2.
