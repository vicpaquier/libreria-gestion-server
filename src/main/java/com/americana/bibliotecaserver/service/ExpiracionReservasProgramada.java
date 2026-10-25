package com.americana.bibliotecaserver.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpiracionReservasProgramada {

    private final ReservaService reservaService;

    public ExpiracionReservasProgramada(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void ejecutar() {
        reservaService.expirarVencidas();
    }
}
