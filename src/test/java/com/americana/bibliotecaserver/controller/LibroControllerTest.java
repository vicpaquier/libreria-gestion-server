package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.LibroDetalleRespuesta;
import com.americana.bibliotecaserver.dto.LibroRespuesta;
import com.americana.bibliotecaserver.dto.PaginaLibrosRespuesta;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.service.LibroService;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibroControllerTest {

    private LibroService libroService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        libroService = mock(LibroService.class);
        var objectMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new LibroController(libroService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void creaCon201YExigeCantidadTotal() throws Exception {
        when(libroService.crear(any())).thenReturn(libro(1L, 3, 3));
        mockMvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Libro","cantidadTotal":3,"autoresIds":[1],"categoriasIds":[2]}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadDisponible").value(3));

        mockMvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Libro","autoresIds":[1],"categoriasIds":[2]}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cantidadTotal"));
    }

    @Test
    void rechazaRelacionesVaciasDuplicadasONoPositivas() throws Exception {
        mockMvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Libro","cantidadTotal":1,"autoresIds":[1,1],"categoriasIds":[2]}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("autoresIds"));
        mockMvc.perform(post("/api/libros").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Libro","cantidadTotal":1,"autoresIds":[0],"categoriasIds":[]}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarNoAceptaCantidadTotalEnSuDto() throws Exception {
        mockMvc.perform(put("/api/libros/1").contentType(MediaType.APPLICATION_JSON).content("""
                {"titulo":"Libro","cantidadTotal":9,"autoresIds":[1],"categoriasIds":[2]}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaConDefaultsYFiltros() throws Exception {
        when(libroService.listar(0, 20, " clean ", 1L, 2L, 3L, 2008, true))
                .thenReturn(new PaginaLibrosRespuesta(0, 20, 0, 0, List.of()));
        mockMvc.perform(get("/api/libros").param("buscar", " clean ")
                        .param("autorId", "1").param("categoriaId", "2")
                        .param("editorialId", "3").param("anio", "2008")
                        .param("disponible", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        verify(libroService).listar(0, 20, " clean ", 1L, 2L, 3L, 2008, true);
    }

    @Test
    void detalleIncluyeActividadVacia() throws Exception {
        when(libroService.consultar(1L)).thenReturn(new LibroDetalleRespuesta(
                1L, "Libro", null, null, null, 1, 1, null,
                List.of(), List.of(), List.of(), List.of()));
        mockMvc.perform(get("/api/libros/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prestamosActivos").isEmpty())
                .andExpect(jsonPath("$.reservasPendientes").isEmpty());
    }

    @Test
    void stockDevuelve409YValidaEntrada() throws Exception {
        when(libroService.ajustarStock(1L, 2)).thenThrow(new ConflictoNegocioException(
                "STOCK_INSUFICIENTE", "cantidadTotal", "Stock insuficiente"));
        mockMvc.perform(patch("/api/libros/1/stock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadTotal\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
        mockMvc.perform(patch("/api/libros/1/stock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidadTotal\":-1}"))
                .andExpect(status().isBadRequest());
    }

    private LibroRespuesta libro(long id, int total, int disponible) {
        return new LibroRespuesta(id, "Libro", null, null, null, total, disponible,
                null, List.of(), List.of());
    }
}
