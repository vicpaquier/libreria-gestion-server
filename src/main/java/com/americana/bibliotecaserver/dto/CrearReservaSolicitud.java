package com.americana.bibliotecaserver.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CrearReservaSolicitud(
        @NotNull @Positive Long usuarioId,
        @NotNull @Positive Long libroId) {
}
