package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.entity.EstadoUsuario;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioSolicitud(
        @NotNull(message = "El estado es obligatorio") EstadoUsuario estado) {
}
