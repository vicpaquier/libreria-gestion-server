package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.PaginaPrestamosRespuesta;
import com.americana.bibliotecaserver.dto.PrestamoRespuesta;
import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.service.PrestamoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PrestamoControllerTest {

    private PrestamoService prestamoService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        prestamoService = mock(PrestamoService.class);
        var objectMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndAddModules()
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new PrestamoController(prestamoService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void creaCon201YValidaFechaIdsUnicosYPositivos() throws Exception {
        when(prestamoService.crear(any())).thenReturn(prestamo());
        mockMvc.perform(post("/api/prestamos").contentType(MediaType.APPLICATION_JSON).content("""
                {"usuarioId":1,"fechaVencimiento":"2026-09-20","librosIds":[5,2]}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.multas[0].id").value(9));

        mockMvc.perform(post("/api/prestamos").contentType(MediaType.APPLICATION_JSON).content("""
                {"usuarioId":1,"librosIds":[2,2]}
                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/prestamos").contentType(MediaType.APPLICATION_JSON).content("""
                {"usuarioId":0,"fechaVencimiento":"2026-09-20","librosIds":[1]}
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noSerializaDiasAtrasoCuandoNoAplica() throws Exception {
        when(prestamoService.listar(0, 20, null)).thenReturn(
                new PaginaPrestamosRespuesta(0, 20, 1, 1, List.of(prestamo())));

        mockMvc.perform(get("/api/prestamos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].diasAtraso").doesNotExist())
                .andExpect(jsonPath("$.content[0].multas[0].prestamoId").value(1));
    }

    @Test
    void listaConDefaultsYEstado() throws Exception {
        when(prestamoService.listar(0, 20, EstadoPrestamo.ACTIVO))
                .thenReturn(new PaginaPrestamosRespuesta(0, 20, 0, 0, List.of()));
        mockMvc.perform(get("/api/prestamos").param("estado", "ACTIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        verify(prestamoService).listar(0, 20, EstadoPrestamo.ACTIVO);
    }

    @Test
    void traduceAusenciaYConflictoYValidaIdsDeRuta() throws Exception {
        when(prestamoService.devolver(1L, 2L))
                .thenThrow(new RecursoNoEncontradoException("Detalle no encontrado"));
        mockMvc.perform(post("/api/prestamos/1/devoluciones/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));

        when(prestamoService.marcarPerdido(1L, 2L)).thenThrow(
                new ConflictoNegocioException("DETALLE_FINALIZADO", "idDetalle", "Finalizado"));
        mockMvc.perform(patch("/api/prestamos/1/detalles/2/perdido"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DETALLE_FINALIZADO"));
        mockMvc.perform(post("/api/prestamos/0/devoluciones/2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void devolucionYPerdidaIncluyenMultas() throws Exception {
        when(prestamoService.devolver(1L, 2L)).thenReturn(prestamo());
        when(prestamoService.marcarPerdido(1L, 3L)).thenReturn(prestamo());

        mockMvc.perform(post("/api/prestamos/1/devoluciones/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multas[0].id").value(9));
        mockMvc.perform(patch("/api/prestamos/1/detalles/3/perdido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.multas[0].id").value(9));
    }

    private PrestamoRespuesta prestamo() {
        return new PrestamoRespuesta(1L, 1L,
                OffsetDateTime.parse("2026-09-08T15:00:00Z"), LocalDate.of(2026, 9, 20),
                EstadoPrestamo.ACTIVO, null, List.of(), List.of(new MultaRespuesta(
                        1L, new BigDecimal("1000.00"), "Atraso", 9L,
                        OffsetDateTime.parse("2026-09-09T15:00:00Z"), EstadoMulta.PENDIENTE)));
    }
}
