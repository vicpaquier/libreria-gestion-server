package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.dto.PaginaMultasRespuesta;
import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.service.MultaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MultaControllerTest {

    private MultaService multaService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        multaService = mock(MultaService.class);
        var objectMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndAddModules()
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new MultaController(multaService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void creaConMontoDecimalYValidaNegativos() throws Exception {
        when(multaService.crear(any())).thenReturn(multa());
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prestamoId\":3,\"monto\":50000.25,\"motivo\":\"Atraso\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.monto").value(50000.25))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prestamoId\":3,\"monto\":-1,\"motivo\":\"Atraso\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prestamoId\":3,\"monto\":1.001,\"motivo\":\"Atraso\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("monto"));
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prestamoId\":3,\"monto\":100000000000000000,\"motivo\":\"Atraso\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("monto"));
    }

    @Test
    void rechazaPropiedadesDesconocidasComoProduccion() throws Exception {
        mockMvc.perform(post("/api/multas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prestamoId\":3,\"monto\":1,\"motivo\":\"Atraso\",\"usuarioId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void pasaTodosLosFiltrosDelListado() throws Exception {
        when(multaService.listar(1, 10, 2L, 3L, EstadoMulta.PENDIENTE,
                LocalDate.of(2026, 9, 8))).thenReturn(
                new PaginaMultasRespuesta(1, 10, 0, 0, List.of()));
        mockMvc.perform(get("/api/multas").param("page", "1").param("size", "10")
                        .param("usuarioId", "2").param("prestamoId", "3")
                        .param("estado", "PENDIENTE").param("fecha", "2026-09-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        verify(multaService).listar(1, 10, 2L, 3L, EstadoMulta.PENDIENTE,
                LocalDate.of(2026, 9, 8));
    }

    private MultaRespuesta multa() {
        return new MultaRespuesta(3L, new BigDecimal("50000.25"), "Atraso", 1L,
                OffsetDateTime.parse("2026-09-08T15:00:00Z"), EstadoMulta.PENDIENTE);
    }
}
