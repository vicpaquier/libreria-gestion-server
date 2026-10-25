package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.entity.EstadoMulta;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MultaRespuesta(
        Long prestamoId,
        BigDecimal monto,
        String motivo,
        Long id,
        OffsetDateTime fechaMulta,
        EstadoMulta estado) {
}
