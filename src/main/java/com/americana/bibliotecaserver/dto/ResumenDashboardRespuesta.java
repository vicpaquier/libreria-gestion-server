package com.americana.bibliotecaserver.dto;

import java.math.BigDecimal;

public record ResumenDashboardRespuesta(
        long libros,
        long ejemplaresDisponibles,
        long prestamosActivos,
        long prestamosAtrasados,
        long reservasPendientes,
        long usuariosActivos,
        BigDecimal multasPendientes) {
}
