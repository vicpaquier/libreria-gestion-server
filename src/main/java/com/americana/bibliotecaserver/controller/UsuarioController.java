package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.ActualizarUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CambiarEstadoUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CrearUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.PaginaUsuariosRespuesta;
import com.americana.bibliotecaserver.dto.UsuarioRespuesta;
import com.americana.bibliotecaserver.dto.UsuarioDetalleRespuesta;
import com.americana.bibliotecaserver.dto.HistorialUsuarioRespuesta;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.service.UsuarioService;
import com.americana.bibliotecaserver.service.HistorialUsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final HistorialUsuarioService historialUsuarioService;

    public UsuarioController(UsuarioService usuarioService,
            HistorialUsuarioService historialUsuarioService) {
        this.usuarioService = usuarioService;
        this.historialUsuarioService = historialUsuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta crear(@Valid @RequestBody CrearUsuarioSolicitud solicitud) {
        return usuarioService.crear(solicitud);
    }

    @GetMapping
    public PaginaUsuariosRespuesta listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false)
            @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado no tiene un valor válido")
            String estado,
            @RequestParam(required = false) String buscar) {
        var estadoUsuario = estado == null ? null : EstadoUsuario.valueOf(estado);
        return usuarioService.listar(page, size, estadoUsuario, buscar);
    }

    @GetMapping("/{id}")
    public UsuarioDetalleRespuesta consultar(@PathVariable @Positive long id) {
        return historialUsuarioService.consultarDetalle(id);
    }

    @GetMapping("/{id}/historial")
    public HistorialUsuarioRespuesta consultarHistorial(@PathVariable @Positive long id) {
        return historialUsuarioService.consultarHistorial(id);
    }

    @PutMapping("/{id}")
    public UsuarioRespuesta actualizar(@PathVariable @Positive long id,
            @Valid @RequestBody ActualizarUsuarioSolicitud solicitud) {
        return usuarioService.actualizar(id, solicitud);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioRespuesta cambiarEstado(@PathVariable @Positive long id,
            @Valid @RequestBody CambiarEstadoUsuarioSolicitud solicitud) {
        return usuarioService.cambiarEstado(id, solicitud);
    }
}
