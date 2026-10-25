package com.americana.bibliotecaserver;

import com.americana.bibliotecaserver.dto.ActualizarUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CambiarEstadoUsuarioSolicitud;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.entity.Usuario;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import com.americana.bibliotecaserver.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers(disabledWithoutDocker = true)
class UsuarioPersistenciaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @BeforeEach
    void limpiarUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void iniciaFlywayYValidaElMapeoJpa() {
        var migracionAplicada = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'",
                Boolean.class);

        var guardado = usuarioRepository.saveAndFlush(
                new Usuario("Ana", "Pérez", "JPA-1", null, null, null));

        assertThat(migracionAplicada).isTrue();
        assertThat(guardado.getId()).isNotNull();
        assertThat(usuarioRepository.findById(guardado.getId())).isPresent();
    }

    @Test
    void aplicaDefaultsDeEstadoYFechaEnPostgresql() {
        var fila = jdbcTemplate.queryForMap("""
                INSERT INTO usuarios (nombre, apellido, cedula)
                VALUES ('Ana', 'Pérez', 'DEFAULTS-1')
                RETURNING estado, fecha_registro
        """);

        assertThat(fila.get("estado")).isEqualTo(EstadoUsuario.ACTIVO.name());
        assertThat(fila.get("fecha_registro")).isNotNull();
    }

    @Test
    void restringeCedulaYEmailDuplicados() {
        insertar("UNICA-1", "unico@example.com");

        assertThatThrownBy(() -> insertar("UNICA-1", "otro@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertar("UNICA-2", "unico@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void permiteMultiplesEmailsNulos() {
        insertar("NULL-1", null);
        insertar("NULL-2", null);

        assertThat(usuarioRepository.count()).isEqualTo(2);
    }

    @Test
    void buscaParcialSinDistinguirMayusculasYOmiteBusquedaVacia() {
        insertar("BUS-1", null, "Juana", "Rojas", EstadoUsuario.ACTIVO);
        insertar("CED-JUAN-2", null, "Carlos", "Ruiz", EstadoUsuario.ACTIVO);
        insertar("BUS-3", null, "Marta", "San Juan", EstadoUsuario.ACTIVO);

        var encontrada = usuarioService.listar(0, 20, null, "  jUaN  ");
        var sinFiltro = usuarioService.listar(0, 20, null, "   ");

        assertThat(encontrada.content()).extracting("cedula")
                .containsExactly("BUS-1", "CED-JUAN-2", "BUS-3");
        assertThat(sinFiltro.totalElements()).isEqualTo(3);
    }

    @Test
    void trataComodinesYBackslashComoLiterales() {
        insertar("ESC-1", null, "Porcentaje%Real", "Uno", EstadoUsuario.ACTIVO);
        insertar("ESC-2", null, "PorcentajeXReal", "Dos", EstadoUsuario.ACTIVO);
        insertar("ESC-3", null, "Guion_Real", "Tres", EstadoUsuario.ACTIVO);
        insertar("ESC-4", null, "GuionXReal", "Cuatro", EstadoUsuario.ACTIVO);
        insertar("ESC-5", null, "Ruta\\Real", "Cinco", EstadoUsuario.ACTIVO);
        insertar("ESC-6", null, "RutaXReal", "Seis", EstadoUsuario.ACTIVO);

        assertThat(usuarioService.listar(0, 20, null, "%").content())
                .extracting("cedula").containsExactly("ESC-1");
        assertThat(usuarioService.listar(0, 20, null, "_").content())
                .extracting("cedula").containsExactly("ESC-3");
        assertThat(usuarioService.listar(0, 20, null, "\\").content())
                .extracting("cedula").containsExactly("ESC-5");
    }

    @Test
    void filtraPorEstado() {
        insertar("EST-1", null, "Ana", "Activa", EstadoUsuario.ACTIVO);
        insertar("EST-2", null, "Inés", "Inactiva", EstadoUsuario.INACTIVO);

        var pagina = usuarioService.listar(0, 20, EstadoUsuario.INACTIVO, null);

        assertThat(pagina.content()).extracting("cedula").containsExactly("EST-2");
        assertThat(pagina.totalElements()).isEqualTo(1);
    }

    @Test
    void paginaEnOrdenDeIdYDevuelveVaciaCuandoEstaFueraDeRango() {
        var primero = insertar("PAG-1", null, "Uno", "A", EstadoUsuario.ACTIVO);
        insertar("PAG-2", null, "Dos", "B", EstadoUsuario.ACTIVO);
        var tercero = insertar("PAG-3", null, "Tres", "C", EstadoUsuario.ACTIVO);
        var cuarto = insertar("PAG-4", null, "Cuatro", "D", EstadoUsuario.ACTIVO);
        insertar("PAG-5", null, "Cinco", "E", EstadoUsuario.ACTIVO);

        var pagina = usuarioService.listar(1, 2, null, null);
        var fueraDeRango = usuarioService.listar(20, 2, null, null);

        assertThat(pagina.content()).extracting("id").containsExactly(tercero, cuarto);
        assertThat(pagina.totalElements()).isEqualTo(5);
        assertThat(pagina.totalPages()).isEqualTo(3);
        assertThat(primero).isLessThan(tercero);
        assertThat(fueraDeRango.content()).isEmpty();
        assertThat(fueraDeRango.page()).isEqualTo(20);
        assertThat(fueraDeRango.totalElements()).isEqualTo(5);
    }

    @Test
    void actualizaDatosEditablesYConservaCamposProtegidos() {
        var id = insertar("ACT-1", "anterior@example.com", "Ana", "Pérez", EstadoUsuario.ACTIVO);
        var fechaRegistro = jdbcTemplate.queryForObject(
                "SELECT fecha_registro FROM usuarios WHERE id_usuario = ?",
                java.time.OffsetDateTime.class,
                id);

        usuarioService.actualizar(id, new ActualizarUsuarioSolicitud(
                "Inés", "Rojas", "ines@example.com", "", " "));

        var fila = jdbcTemplate.queryForMap("SELECT * FROM usuarios WHERE id_usuario = ?", id);
        assertThat(fila.get("nombre")).isEqualTo("Inés");
        assertThat(fila.get("apellido")).isEqualTo("Rojas");
        assertThat(fila.get("email")).isEqualTo("ines@example.com");
        assertThat(fila.get("telefono")).isEqualTo("");
        assertThat(fila.get("direccion")).isEqualTo(" ");
        assertThat(fila.get("cedula")).isEqualTo("ACT-1");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT fecha_registro FROM usuarios WHERE id_usuario = ?",
                java.time.OffsetDateTime.class,
                id)).isEqualTo(fechaRegistro);
        assertThat(fila.get("estado")).isEqualTo(EstadoUsuario.ACTIVO.name());
    }

    @Test
    void cambiaEstadoUsandoLaConsultaConBloqueoDePostgresql() {
        var id = insertar("ESTADO-1", null, "Ana", "Pérez", EstadoUsuario.ACTIVO);

        var respuesta = usuarioService.cambiarEstado(
                id, new CambiarEstadoUsuarioSolicitud(EstadoUsuario.INACTIVO));

        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT estado FROM usuarios WHERE id_usuario = ?", String.class, id))
                .isEqualTo(EstadoUsuario.INACTIVO.name());
    }

    @Test
    void rechazaEmailDeOtroUsuarioAlActualizar() {
        insertar("EMAIL-1", "usado@example.com", "Ana", "Pérez", EstadoUsuario.ACTIVO);
        var id = insertar("EMAIL-2", null, "Inés", "Rojas", EstadoUsuario.ACTIVO);

        assertThatThrownBy(() -> usuarioService.actualizar(id, new ActualizarUsuarioSolicitud(
                "Inés", "Rojas", "usado@example.com", null, null)))
                .isInstanceOf(com.americana.bibliotecaserver.exception.ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("email");
    }

    private void insertar(String cedula, String email) {
        jdbcTemplate.update("""
                INSERT INTO usuarios (nombre, apellido, cedula, email)
                VALUES (?, ?, ?, ?)
                """, "Ana", "Pérez", cedula, email);
    }

    private long insertar(String cedula, String email, String nombre, String apellido,
            EstadoUsuario estado) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO usuarios (nombre, apellido, cedula, email, estado)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id_usuario
                """, Long.class, nombre, apellido, cedula, email, estado.name());
    }
}
