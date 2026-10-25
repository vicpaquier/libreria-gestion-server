# Guía del backend

## Proyecto

- Aplicación Java 25 con Spring Boot 4.1.1 bajo el paquete
  `com.americana.bibliotecaserver`.
- El proyecto usa Spring MVC, Spring Data JPA y PostgreSQL.
- La aplicación está en una fase inicial; crear sólo las capas necesarias para el caso de uso.
- El contrato compartido está en `../docs/openapi.yaml` y las reglas en `../docs/specs.md`.

## Arquitectura

- Organizar por capas bajo el paquete base: `controller`, `service`, `repository`, `entity`,
  `dto`, `mapper`, `exception`, `validation` y `config` cuando hagan falta.
- Mantener controladores delgados y reglas de negocio en servicios.
- Usar DTOs de entrada y salida; no exponer entidades JPA.
- Usar Bean Validation en entradas. Agregar el starter de validación antes de usar anotaciones,
  porque aún no está en `pom.xml`.
- Usar `java.time` para fechas y `BigDecimal` para multas.
- Mantener restricciones únicas y checks críticos tanto en aplicación como en PostgreSQL.
- No copiar sintaxis SQLite de `bd_schema.txt`; traducirla a PostgreSQL mediante migraciones.

## Transacciones

- Crear préstamos, devolver detalles, marcar pérdidas y ajustar stock son operaciones
  transaccionales.
- Bloquear o proteger explícitamente el stock frente a préstamos concurrentes.
- Un error en cualquier libro debe revertir el préstamo completo.
- Los jobs programados deben invocar servicios idempotentes, no contener las reglas directamente.

## API

- Respetar rutas, métodos, estados y DTOs de `../docs/openapi.yaml`.
- Usar paginación en listados indicados por los specs y filtros en consultas de repositorio.
- Centralizar errores y devolver el esquema `ApiError` del contrato.
- Usar `400`, `404` y `409` de forma consistente para entrada inválida, ausencia y conflicto.

## Comandos

Ejecutar siempre con el Maven Wrapper:

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw verify
```

No inventar configuración de base de datos. Las credenciales deben llegar por variables de
entorno o configuración local no versionada.
