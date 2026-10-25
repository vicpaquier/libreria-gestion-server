package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.AutorRespuesta;
import com.americana.bibliotecaserver.dto.AutorSolicitud;
import com.americana.bibliotecaserver.service.AutorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/autores")
public class AutorController {

    private final AutorService autorService;

    public AutorController(AutorService autorService) {
        this.autorService = autorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AutorRespuesta crear(@Valid @RequestBody AutorSolicitud solicitud) {
        return autorService.crear(solicitud);
    }

    @GetMapping
    public List<AutorRespuesta> listar(@RequestParam(required = false) String buscar) {
        return autorService.listar(buscar);
    }

    @PutMapping("/{id}")
    public AutorRespuesta actualizar(@PathVariable @Positive long id,
            @Valid @RequestBody AutorSolicitud solicitud) {
        return autorService.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @Positive long id) {
        autorService.eliminar(id);
    }
}
