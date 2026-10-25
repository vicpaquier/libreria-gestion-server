package com.americana.bibliotecaserver;

import com.americana.bibliotecaserver.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class CategoriaPersistenciaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void limpiarCategorias() {
        categoriaRepository.deleteAll();
    }

    @Test
    void aplicaV4IdentityYRestriccionUnicaNombrada() {
        var migracionAplicada = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '4'", Boolean.class);
        var restriccion = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_constraint
                WHERE conname = 'uq_categorias_nombre' AND contype = 'u'
                """, Integer.class);
        var id = insertar("Migración", null);

        assertThat(migracionAplicada).isTrue();
        assertThat(restriccion).isOne();
        assertThat(id).isPositive();
    }

    @Test
    void creaListaActualizaYEliminaConDescripcionOpcionalYOrdenPorId() throws Exception {
        var primero = insertar("Primera", "Descripción");
        var segundo = insertar("Segunda", null);

        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(primero))
                .andExpect(jsonPath("$[1].id").value(segundo));

        var creada = mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  Tercera  \",\"descripcion\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Tercera"))
                .andExpect(jsonPath("$.descripcion").doesNotExist())
                .andReturn();
        var idCreada = Long.parseLong(com.jayway.jsonpath.JsonPath.read(
                creada.getResponse().getContentAsString(), "$.id").toString());

        mockMvc.perform(put("/api/categorias/{id}", idCreada)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Tercera\",\"descripcion\":\" Narrativa \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").value("Narrativa"));
        mockMvc.perform(delete("/api/categorias/{id}", idCreada))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(categoriaRepository.findById(idCreada)).isEmpty();
    }

    @Test
    void aplicaUnicidadExactaTrasTrimYErroresDelContrato() throws Exception {
        var novela = insertar("Novela", null);
        var ensayo = insertar("Ensayo", null);

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Novela \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_CATEGORIA_DUPLICADO"));
        mockMvc.perform(put("/api/categorias/{id}", ensayo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Novela \"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/categorias/{id}", novela)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Novela \"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"novela\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categorias/999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("nombre"));
        mockMvc.perform(put("/api/categorias/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Válida\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("id"));
    }

    private long insertar(String nombre, String descripcion) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO categorias (nombre, descripcion)
                VALUES (?, ?)
                RETURNING id_categoria
                """, Long.class, nombre, descripcion);
    }
}
