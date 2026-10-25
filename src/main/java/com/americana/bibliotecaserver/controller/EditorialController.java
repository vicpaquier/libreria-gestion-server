package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.EditorialRespuesta;
import com.americana.bibliotecaserver.dto.EditorialSolicitud;
import com.americana.bibliotecaserver.service.EditorialService;
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
@RequestMapping("/api/editoriales")
public class EditorialController {

    private final EditorialService editorialService;

    public EditorialController(EditorialService editorialService) {
        this.editorialService = editorialService;
    }

    @GetMapping
    public List<EditorialRespuesta> listar() {
        return editorialService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EditorialRespuesta crear(@Valid @RequestBody EditorialSolicitud solicitud) {
        return editorialService.crear(solicitud);
    }

    @PutMapping("/{id}")
    public EditorialRespuesta actualizar(@PathVariable @Positive long id,
            @Valid @RequestBody EditorialSolicitud solicitud) {
        return editorialService.actualizar(id, solicitud);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @Positive long id) {
        editorialService.eliminar(id);
    }
}
