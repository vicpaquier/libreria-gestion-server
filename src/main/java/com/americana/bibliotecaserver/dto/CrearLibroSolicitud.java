package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.validation.ElementosUnicos;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CrearLibroSolicitud(
        @NotBlank(message = "El título es obligatorio") String titulo,
        String isbn,
        Integer anioPublicacion,
        @Positive(message = "El número de páginas debe ser positivo") Integer numeroPaginas,
        @NotNull(message = "La cantidad total es obligatoria")
        @Min(value = 0, message = "La cantidad total no puede ser negativa") Integer cantidadTotal,
        @Positive(message = "La editorial debe ser positiva") Long editorialId,
        @NotEmpty(message = "Debe indicar al menos un autor") @ElementosUnicos
        List<@NotNull @Positive Long> autoresIds,
        @NotEmpty(message = "Debe indicar al menos una categoría") @ElementosUnicos
        List<@NotNull @Positive Long> categoriasIds) {
}
