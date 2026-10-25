package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.LibroResumenRespuesta;
import com.americana.bibliotecaserver.dto.ReservaRespuesta;
import com.americana.bibliotecaserver.entity.EstadoReserva;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservaControllerTest {

    private ReservaService reservaService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        reservaService = mock(ReservaService.class);
        var objectMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndAddModules()
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservaController(reservaService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void creaConDtoDelContratoYExpiracionNula() throws Exception {
        when(reservaService.crear(any())).thenReturn(reserva(EstadoReserva.PENDIENTE));
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1,\"libroId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libro.id").value(2))
                .andExpect(jsonPath("$.fechaExpiracion").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void validaIdsYTraduceTransicionInvalida() throws Exception {
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":0,\"libroId\":2}"))
                .andExpect(status().isBadRequest());
        when(reservaService.atender(3L)).thenThrow(new ConflictoNegocioException(
                "RESERVA_NO_PENDIENTE", "id", "No pendiente"));
        mockMvc.perform(patch("/api/reservas/3/atender"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RESERVA_NO_PENDIENTE"));
    }

    @Test
    void rechazaPropiedadesDesconocidasComoProduccion() throws Exception {
        mockMvc.perform(post("/api/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuarioId\":1,\"libroId\":2,\"fechaExpiracion\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    private ReservaRespuesta reserva(EstadoReserva estado) {
        return new ReservaRespuesta(1L, 1L, new LibroResumenRespuesta(2L, "Libro", null),
                OffsetDateTime.parse("2026-09-08T15:00:00Z"), null, estado);
    }
}
