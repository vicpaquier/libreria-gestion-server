package com.americana.bibliotecaserver;

import com.americana.bibliotecaserver.repository.EditorialRepository;
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
class EditorialPersistenciaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EditorialRepository editorialRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void limpiarEditoriales() {
        editorialRepository.deleteAll();
    }

    @Test
    void aplicaMigracionIdentityYRestriccionUnicaNombrada() {
        var migracionAplicada = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '3'", Boolean.class);
        var restriccion = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_constraint
                WHERE conname = 'uq_editoriales_nombre' AND contype = 'u'
                """, Integer.class);
        var id = insertar("Migración", null);

        assertThat(migracionAplicada).isTrue();
        assertThat(restriccion).isOne();
        assertThat(id).isPositive();
    }

    @Test
    void creaListaActualizaYEliminaConPaisOpcionalYOrdenPorId() throws Exception {
        var primero = insertar("Primera", "Colombia");
        var segundo = insertar("Segunda", null);

        mockMvc.perform(get("/api/editoriales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(primero))
                .andExpect(jsonPath("$[1].id").value(segundo));

        var creada = mockMvc.perform(post("/api/editoriales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"  Tercera  \",\"pais\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Tercera"))
                .andExpect(jsonPath("$.pais").doesNotExist())
                .andReturn();
        var idCreada = Long.parseLong(com.jayway.jsonpath.JsonPath.read(
                creada.getResponse().getContentAsString(), "$.id").toString());

        mockMvc.perform(put("/api/editoriales/{id}", idCreada)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Tercera\",\"pais\":\" Argentina \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pais").value("Argentina"));
        mockMvc.perform(delete("/api/editoriales/{id}", idCreada))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        assertThat(editorialRepository.findById(idCreada)).isEmpty();
    }

    @Test
    void aplicaUnicidadExactaTrasTrimEnPostYPutYPermiteMismoRegistro() throws Exception {
        var norma = insertar("Norma", null);
        var planeta = insertar("Planeta", null);

        mockMvc.perform(post("/api/editoriales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Norma \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_EDITORIAL_DUPLICADO"))
                .andExpect(jsonPath("$.campo").value("nombre"));
        mockMvc.perform(put("/api/editoriales/{id}", planeta)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Norma \"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("nombre"));
        mockMvc.perform(put("/api/editoriales/{id}", norma)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" Norma \",\"pais\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Norma"));

        mockMvc.perform(post("/api/editoriales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"norma\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void devuelve404Y400DesdeLaAplicacionCompleta() throws Exception {
        mockMvc.perform(delete("/api/editoriales/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));
        mockMvc.perform(post("/api/editoriales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("nombre"));
        mockMvc.perform(put("/api/editoriales/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Válida\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("id"));
    }

    private long insertar(String nombre, String pais) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO editoriales (nombre, pais)
                VALUES (?, ?)
                RETURNING id_editorial
                """, Long.class, nombre, pais);
    }
}
