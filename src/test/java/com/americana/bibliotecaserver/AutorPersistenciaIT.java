package com.americana.bibliotecaserver;

import com.americana.bibliotecaserver.dto.AutorSolicitud;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.repository.AutorRepository;
import com.americana.bibliotecaserver.service.AutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers(disabledWithoutDocker = true)
class AutorPersistenciaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private AutorService autorService;

    @BeforeEach
    void limpiarAutores() {
        autorRepository.deleteAll();
    }

    @Test
    void aplicaMigracionYGeneraIdIdentity() {
        var migracionAplicada = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '2'",
                Boolean.class);

        var respuesta = autorService.crear(new AutorSolicitud("Ana", "Rojas", null));

        assertThat(migracionAplicada).isTrue();
        assertThat(respuesta.id()).isNotNull();
        assertThat(respuesta.nacionalidad()).isNull();
    }

    @Test
    void buscaParcialCaseInsensitiveEnTodosLosCamposYOrdenaPorId() {
        var primero = insertar("Gabriel", "García Márquez", "Colombiana");
        var segundo = insertar("Colombia", "Paz", "Argentina");
        var tercero = insertar("Ana", "Rojas", "COLOMBIANA");

        assertThat(autorService.listar("  cOlOmBiA  ")).extracting("id")
                .containsExactly(primero, segundo, tercero);
        assertThat(autorService.listar("garcía")).extracting("id")
                .containsExactly(primero);
        assertThat(autorService.listar("   ")).hasSize(3);
    }

    @Test
    void trataBackslashPorcentajeYGuionBajoComoLiterales() {
        var porcentaje = insertar("Porcentaje%Real", "Uno", null);
        insertar("PorcentajeXReal", "Dos", null);
        var guion = insertar("Guion_Real", "Tres", null);
        insertar("GuionXReal", "Cuatro", null);
        var backslash = insertar("Ruta\\Real", "Cinco", null);
        insertar("RutaXReal", "Seis", null);

        assertThat(autorService.listar("%")).extracting("id").containsExactly(porcentaje);
        assertThat(autorService.listar("_")).extracting("id").containsExactly(guion);
        assertThat(autorService.listar("\\")).extracting("id").containsExactly(backslash);
    }

    @Test
    void actualizaEliminaYRespondeNoEncontrado() {
        var id = insertar("Ana", "Rojas", null);

        var actualizado = autorService.actualizar(
                id, new AutorSolicitud("Inés", "Paz", "Argentina"));

        assertThat(actualizado.nombre()).isEqualTo("Inés");
        assertThat(actualizado.apellido()).isEqualTo("Paz");
        assertThat(actualizado.nacionalidad()).isEqualTo("Argentina");

        autorService.eliminar(id);

        assertThat(autorRepository.findById(id)).isEmpty();
        assertThatThrownBy(() -> autorService.eliminar(id))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> autorService.actualizar(
                id, new AutorSolicitud("Otra", "Persona", null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private long insertar(String nombre, String apellido, String nacionalidad) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO autores (nombre, apellido, nacionalidad)
                VALUES (?, ?, ?)
                RETURNING id_autor
                """, Long.class, nombre, apellido, nacionalidad);
    }
}
