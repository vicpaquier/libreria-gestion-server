package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.CrearMultaSolicitud;
import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.dto.PaginaMultasRespuesta;
import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.service.MultaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;

@RestController
@RequestMapping("/api/multas")
public class MultaController {

    private final MultaService multaService;

    public MultaController(MultaService multaService) {
        this.multaService = multaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MultaRespuesta crear(@Valid @RequestBody CrearMultaSolicitud solicitud) {
        return multaService.crear(solicitud);
    }

    @GetMapping
    public PaginaMultasRespuesta listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Positive Long usuarioId,
            @RequestParam(required = false) @Positive Long prestamoId,
            @RequestParam(required = false) EstadoMulta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {
        return multaService.listar(page, size, usuarioId, prestamoId, estado, fecha);
    }

    @PatchMapping("/{id}/pagar")
    public MultaRespuesta pagar(@PathVariable @Positive long id) {
        return multaService.pagar(id);
    }

    @PatchMapping("/{id}/cancelar")
    public MultaRespuesta cancelar(@PathVariable @Positive long id) {
        return multaService.cancelar(id);
    }
}
