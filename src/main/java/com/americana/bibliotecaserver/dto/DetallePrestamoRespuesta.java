package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.entity.EstadoDetallePrestamo;

import java.time.OffsetDateTime;

public record DetallePrestamoRespuesta(
        Long id,
        LibroResumenRespuesta libro,
        OffsetDateTime fechaDevolucion,
        EstadoDetallePrestamo estado) {
}
