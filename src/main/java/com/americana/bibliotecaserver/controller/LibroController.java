package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.ActualizarLibroSolicitud;
import com.americana.bibliotecaserver.dto.AjustarStockLibroSolicitud;
import com.americana.bibliotecaserver.dto.CrearLibroSolicitud;
import com.americana.bibliotecaserver.dto.LibroDetalleRespuesta;
import com.americana.bibliotecaserver.dto.LibroRespuesta;
import com.americana.bibliotecaserver.dto.PaginaLibrosRespuesta;
import com.americana.bibliotecaserver.service.LibroService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService libroService;

    public LibroController(LibroService libroService) {
        this.libroService = libroService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LibroRespuesta crear(@Valid @RequestBody CrearLibroSolicitud solicitud) {
        return libroService.crear(solicitud);
    }

    @GetMapping
    public PaginaLibrosRespuesta listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) @Positive Long autorId,
            @RequestParam(required = false) @Positive Long categoriaId,
            @RequestParam(required = false) @Positive Long editorialId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Boolean disponible) {
        return libroService.listar(page, size, buscar, autorId, categoriaId, editorialId,
                anio, disponible);
    }

    @GetMapping("/{id}")
    public LibroDetalleRespuesta consultar(@PathVariable @Positive long id) {
        return libroService.consultar(id);
    }

    @PutMapping("/{id}")
    public LibroRespuesta actualizar(@PathVariable @Positive long id,
            @Valid @RequestBody ActualizarLibroSolicitud solicitud) {
        return libroService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/stock")
    public LibroRespuesta ajustarStock(@PathVariable @Positive long id,
            @Valid @RequestBody AjustarStockLibroSolicitud solicitud) {
        return libroService.ajustarStock(id, solicitud.cantidadTotal());
    }
}
