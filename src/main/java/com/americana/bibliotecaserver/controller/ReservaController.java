package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.CrearReservaSolicitud;
import com.americana.bibliotecaserver.dto.ReservaRespuesta;
import com.americana.bibliotecaserver.service.ReservaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaRespuesta crear(@Valid @RequestBody CrearReservaSolicitud solicitud) {
        return reservaService.crear(solicitud);
    }

    @PatchMapping("/{id}/atender")
    public ReservaRespuesta atender(@PathVariable @Positive long id) {
        return reservaService.atender(id);
    }

    @PatchMapping("/{id}/cancelar")
    public ReservaRespuesta cancelar(@PathVariable @Positive long id) {
        return reservaService.cancelar(id);
    }
}
