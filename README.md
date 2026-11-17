# Backend - Gestión de biblioteca

API REST de la aplicación de biblioteca, desarrollada con Java 25, Spring Boot 4, Spring MVC,
Spring Data JPA, Flyway y PostgreSQL.

## Requisitos

- JDK 25.
- PostgreSQL accesible desde el servidor de la aplicación.
- Docker, únicamente si se ejecutan pruebas de integración que usan Testcontainers.

No es necesario instalar Maven: el proyecto incluye Maven Wrapper.

## Base de datos

El perfil predeterminado es `postgres`. La aplicación valida el modelo con Hibernate y Flyway
ejecuta automáticamente las migraciones de `src/main/resources/db/migration/` al iniciar.

La configuración predeterminada utiliza el esquema `libreria`; se debe crear una vez antes del
primer arranque:

```sql
CREATE SCHEMA IF NOT EXISTS libreria;
```

El usuario configurado para la aplicación debe tener permisos para usar el esquema y crear o
modificar sus objetos durante las migraciones.

## Variables de entorno

| Variable      | Descripción               | Valor local predeterminado                                         |
| ------------- | ------------------------- | ------------------------------------------------------------------ |
| `DB_URL`      | URL JDBC de PostgreSQL.   | `jdbc:postgresql://localhost:5435/postgres?currentSchema=libreria` |
| `DB_USERNAME` | Usuario de PostgreSQL.    | `postgres`                                                         |
| `DB_PASSWORD` | Contraseña de PostgreSQL. | `1`                                                                |

Los valores predeterminados son únicamente para desarrollo. En un despliegue se deben definir las
tres variables con credenciales propias y mantener `currentSchema=libreria` en `DB_URL`, salvo que
también se cambie la configuración del esquema.

Ejemplo para Linux o macOS:

```bash
export DB_URL='jdbc:postgresql://db.example.com:5432/biblioteca?currentSchema=libreria'
export DB_USERNAME='biblioteca_app'
export DB_PASSWORD='cambiar-por-un-secreto-seguro'
```

En PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://db.example.com:5432/biblioteca?currentSchema=libreria'
$env:DB_USERNAME = 'biblioteca_app'
$env:DB_PASSWORD = 'cambiar-por-un-secreto-seguro'
```

## Ejecución local

Linux o macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080/api`.

Para una ejecución temporal sin PostgreSQL se puede activar el perfil H2:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

H2 usa una base de datos en memoria; su contenido se elimina al detener la aplicación y no se debe
usar en producción.

## Pruebas

```bash
./mvnw test
./mvnw verify
```

`verify` también ejecuta las pruebas de integración. Docker debe estar disponible para las pruebas
basadas en Testcontainers.

## Compilación para producción

```bash
./mvnw clean verify
```

El artefacto ejecutable se genera en:

```text
target/bibliotecaserver-0.0.1-SNAPSHOT.jar
```

## Despliegue

1. Crear la base de datos y el esquema `libreria` en PostgreSQL.
2. Configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` como secretos o variables del entorno de
   ejecución.
3. Compilar el JAR con `./mvnw clean verify` o hacerlo en la etapa de construcción de la plataforma.
4. Iniciar la aplicación con Java 25.
5. Configurar el servidor del frontend para enviar las solicitudes de `/api/*` a este servicio.

Ejecución del JAR en el puerto predeterminado `8080`:

```bash
java -jar target/bibliotecaserver-0.0.1-SNAPSHOT.jar
```

Para usar otro puerto, por ejemplo el asignado por una plataforma de despliegue:

```bash
java -jar target/bibliotecaserver-0.0.1-SNAPSHOT.jar --server.port="${PORT:-8080}"
```

Las migraciones se aplican durante el arranque. Si una migración o la conexión con PostgreSQL
falla, la aplicación no debe exponerse al tráfico hasta corregir el error.

### Ejemplo como servicio de systemd

```ini
[Unit]
Description=API de gestión de biblioteca
After=network.target

[Service]
User=biblioteca
WorkingDirectory=/opt/bibliotecaserver
EnvironmentFile=/etc/bibliotecaserver.env
ExecStart=/usr/bin/java -jar /opt/bibliotecaserver/bibliotecaserver.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

El archivo `/etc/bibliotecaserver.env` debe contener las variables de la base de datos y tener
permisos restringidos. Después de instalar el JAR y la unidad:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now bibliotecaserver
sudo systemctl status bibliotecaserver
```
