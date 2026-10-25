package com.americana.bibliotecaserver.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeteccionAtrasosProgramada {

    private final PrestamoService prestamoService;

    public DeteccionAtrasosProgramada(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void ejecutar() {
        prestamoService.detectarAtrasados();
    }
}
