package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.CategoriaRespuesta;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.service.CategoriaService;
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

class CategoriaControllerTest {

    private CategoriaService categoriaService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        categoriaService = mock(CategoriaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoriaController(categoriaService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void exponeCrudConEstadosDelContrato() throws Exception {
        when(categoriaService.crear(any()))
                .thenReturn(new CategoriaRespuesta(1L, "Novela", "Narrativa"));
        when(categoriaService.listar())
                .thenReturn(List.of(new CategoriaRespuesta(1L, "Novela", "Narrativa")));
        when(categoriaService.actualizar(eq(1L), any()))
                .thenReturn(new CategoriaRespuesta(1L, "Novela", null));

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Novela\",\"descripcion\":\"Narrativa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Novela"));
        mockMvc.perform(put("/api/categorias/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Novela\",\"descripcion\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcion").doesNotExist());
        mockMvc.perform(delete("/api/categorias/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        verify(categoriaService).eliminar(1L);
    }

    @Test
    void devuelve400ParaNombreEnBlancoEIdNoPositivo() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("nombre"));
        mockMvc.perform(delete("/api/categorias/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("id"));
    }

    @Test
    void devuelve409Y404ComoApiError() throws Exception {
        when(categoriaService.crear(any())).thenThrow(new ConflictoUnicidadException(
                "NOMBRE_CATEGORIA_DUPLICADO", "nombre",
                "El nombre de la categoría ya está registrado"));
        when(categoriaService.actualizar(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Categoría no encontrada"));

        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Novela\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_CATEGORIA_DUPLICADO"))
                .andExpect(jsonPath("$.campo").value("nombre"));
        mockMvc.perform(put("/api/categorias/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Novela\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensaje").value("Categoría no encontrada"));
    }
}
