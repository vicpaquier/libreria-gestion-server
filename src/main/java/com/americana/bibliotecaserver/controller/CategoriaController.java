package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.CategoriaRespuesta;
import com.americana.bibliotecaserver.dto.CategoriaSolicitud;
import com.americana.bibliotecaserver.service.CategoriaService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaRespuesta> listar() {
        return categoriaService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaRespuesta crear(@Valid @RequestBody CategoriaSolicitud solicitud) {
        return categoriaService.crear(solicitud);
    }

    @PutMapping("/{id}")
    public CategoriaRespuesta actualizar(@PathVariable @Positive long id,
            @Valid @RequestBody CategoriaSolicitud solicitud) {
        return categoriaService.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @Positive long id) {
        categoriaService.eliminar(id);
    }
}
