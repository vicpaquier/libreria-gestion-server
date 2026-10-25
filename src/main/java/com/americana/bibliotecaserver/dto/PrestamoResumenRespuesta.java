package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.entity.EstadoPrestamo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PrestamoResumenRespuesta(
        Long id,
        Long usuarioId,
        OffsetDateTime fechaPrestamo,
        LocalDate fechaVencimiento,
        EstadoPrestamo estado,
        Integer diasAtraso) {
}
