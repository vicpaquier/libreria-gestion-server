package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.EditorialRespuesta;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.service.EditorialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EditorialControllerTest {

    private EditorialService editorialService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        editorialService = mock(EditorialService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EditorialController(editorialService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void exponeCrudConEstadosDelContrato() throws Exception {
        when(editorialService.crear(any()))
                .thenReturn(new EditorialRespuesta(1L, "Norma", "Colombia"));
        when(editorialService.listar())
                .thenReturn(List.of(new EditorialRespuesta(1L, "Norma", "Colombia")));
        when(editorialService.actualizar(eq(1L), any()))
                .thenReturn(new EditorialRespuesta(1L, "Norma", null));

        mockMvc.perform(post("/api/editoriales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Norma\",\"pais\":\"Colombia\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/api/editoriales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Norma"));
        mockMvc.perform(put("/api/editoriales/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Norma\",\"pais\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pais").doesNotExist());
        mockMvc.perform(delete("/api/editoriales/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(editorialService).eliminar(1L);
    }

    @Test
    void devuelve400ParaNombreEnBlancoEIdNoPositivo() throws Exception {
        mockMvc.perform(post("/api/editoriales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \",\"pais\":\"Colombia\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("nombre"));
        mockMvc.perform(delete("/api/editoriales/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("id"));
    }

    @Test
    void devuelveApiError409ConCodigoYCampo() throws Exception {
        when(editorialService.crear(any())).thenThrow(new ConflictoUnicidadException(
                "NOMBRE_EDITORIAL_DUPLICADO", "nombre",
                "El nombre de la editorial ya está registrado"));

        mockMvc.perform(post("/api/editoriales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Norma\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_EDITORIAL_DUPLICADO"))
                .andExpect(jsonPath("$.campo").value("nombre"));
    }

    @Test
    void devuelveApiError404() throws Exception {
        when(editorialService.actualizar(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Editorial no encontrada"));

        mockMvc.perform(put("/api/editoriales/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Norma\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("Editorial no encontrada"));
    }
}
