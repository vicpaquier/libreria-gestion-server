package com.americana.bibliotecaserver.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PrestamoRespuesta(
        Long id,
        Long usuarioId,
        OffsetDateTime fechaPrestamo,
        LocalDate fechaVencimiento,
        EstadoPrestamo estado,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer diasAtraso,
        List<DetallePrestamoRespuesta> detalles,
        List<MultaRespuesta> multas) {
}
