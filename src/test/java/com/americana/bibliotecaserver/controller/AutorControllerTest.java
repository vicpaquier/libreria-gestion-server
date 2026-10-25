package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.AutorRespuesta;
import com.americana.bibliotecaserver.dto.AutorSolicitud;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.service.AutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

class AutorControllerTest {

    private AutorService autorService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        autorService = mock(AutorService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AutorController(autorService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void responde201ConAutorCreado() throws Exception {
        when(autorService.crear(any())).thenReturn(
                new AutorRespuesta(1L, "Gabriel", "García Márquez", "Colombiana"));

        mockMvc.perform(post("/api/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Gabriel","apellido":"García Márquez","nacionalidad":"Colombiana"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nacionalidad").value("Colombiana"));
        verify(autorService).crear(any(AutorSolicitud.class));
    }

    @Test
    void rechazaNombreYApellidoEnBlanco() throws Exception {
        mockMvc.perform(post("/api/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","apellido":"Rojas"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("nombre"));

        mockMvc.perform(post("/api/autores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("apellido"));
    }

    @Test
    void listaArrayYPasaBusquedaSinModificar() throws Exception {
        when(autorService.listar(" García ")).thenReturn(List.of(
                new AutorRespuesta(1L, "Gabriel", "García Márquez", null)));

        mockMvc.perform(get("/api/autores").param("buscar", " García "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apellido").value("García Márquez"));
        verify(autorService).listar(" García ");
    }

    @Test
    void actualizaAutor() throws Exception {
        when(autorService.actualizar(org.mockito.ArgumentMatchers.eq(3L), any())).thenReturn(
                new AutorRespuesta(3L, "Ana", "Paz", null));

        mockMvc.perform(put("/api/autores/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Paz","nacionalidad":null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.apellido").value("Paz"));
    }

    @Test
    void eliminaAutorSinContenido() throws Exception {
        mockMvc.perform(delete("/api/autores/3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(autorService).eliminar(3L);
    }

    @Test
    void rechazaIdsNoPositivos() throws Exception {
        mockMvc.perform(put("/api/autores/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Paz"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("id"));

        mockMvc.perform(delete("/api/autores/-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("id"));
    }

    @Test
    void devuelveApiError404Reutilizable() throws Exception {
        when(autorService.actualizar(org.mockito.ArgumentMatchers.eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Autor no encontrado"));

        mockMvc.perform(put("/api/autores/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Paz"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("Autor no encontrado"))
                .andExpect(jsonPath("$.path").value("/api/autores/99"));
    }
}
