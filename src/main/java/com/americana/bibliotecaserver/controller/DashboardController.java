package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.ResumenDashboardRespuesta;
import com.americana.bibliotecaserver.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumen")
    public ResumenDashboardRespuesta consultar() {
        return dashboardService.consultar();
    }
}
