package com.americana.bibliotecaserver.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ExpiracionReservasProgramadaTest {

    @Test
    void elSchedulerSoloDisparaElCasoDeUso() {
        var reservaService = mock(ReservaService.class);

        new ExpiracionReservasProgramada(reservaService).ejecutar();

        verify(reservaService).expirarVencidas();
    }
}
