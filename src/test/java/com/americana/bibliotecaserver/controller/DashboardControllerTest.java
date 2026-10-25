package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.ResumenDashboardRespuesta;
import com.americana.bibliotecaserver.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    @Test
    void respondeConTodosLosCamposDelResumen() throws Exception {
        var servicio = mock(DashboardService.class);
        when(servicio.consultar()).thenReturn(new ResumenDashboardRespuesta(
                10, 24, 3, 2, 4, 8, new BigDecimal("75000.50")));
        var mvc = MockMvcBuilders.standaloneSetup(new DashboardController(servicio)).build();

        mvc.perform(get("/api/dashboard/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libros").value(10))
                .andExpect(jsonPath("$.ejemplaresDisponibles").value(24))
                .andExpect(jsonPath("$.multasPendientes").value(75000.50));
    }
}
