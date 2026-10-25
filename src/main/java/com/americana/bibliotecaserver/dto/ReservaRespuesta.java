package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.entity.EstadoReserva;

import java.time.OffsetDateTime;

public record ReservaRespuesta(
        Long id,
        Long usuarioId,
        LibroResumenRespuesta libro,
        OffsetDateTime fechaReserva,
        OffsetDateTime fechaExpiracion,
        EstadoReserva estado) {
}
