package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.validation.ElementosUnicos;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

public record CrearPrestamoSolicitud(
        @NotNull @Positive Long usuarioId,
        @NotNull LocalDate fechaVencimiento,
        @NotEmpty @ElementosUnicos List<@NotNull @Positive Long> librosIds) {
}
