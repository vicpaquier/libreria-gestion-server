package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.CrearPrestamoSolicitud;
import com.americana.bibliotecaserver.dto.PaginaPrestamosRespuesta;
import com.americana.bibliotecaserver.dto.PrestamoRespuesta;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.service.PrestamoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService prestamoService;

    public PrestamoController(PrestamoService prestamoService) {
        this.prestamoService = prestamoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrestamoRespuesta crear(@Valid @RequestBody CrearPrestamoSolicitud solicitud) {
        return prestamoService.crear(solicitud);
    }

    @GetMapping
    public PaginaPrestamosRespuesta listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) EstadoPrestamo estado) {
        return prestamoService.listar(page, size, estado);
    }

    @PostMapping("/{idPrestamo}/devoluciones/{idDetalle}")
    public PrestamoRespuesta devolver(@PathVariable @Positive long idPrestamo,
            @PathVariable @Positive long idDetalle) {
        return prestamoService.devolver(idPrestamo, idDetalle);
    }

    @PatchMapping("/{idPrestamo}/detalles/{idDetalle}/perdido")
    public PrestamoRespuesta marcarPerdido(@PathVariable @Positive long idPrestamo,
            @PathVariable @Positive long idDetalle) {
        return prestamoService.marcarPerdido(idPrestamo, idDetalle);
    }
}
