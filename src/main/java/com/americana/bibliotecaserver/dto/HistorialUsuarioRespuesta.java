package com.americana.bibliotecaserver.dto;

import java.util.List;

public record HistorialUsuarioRespuesta(
        List<PrestamoRespuesta> prestamos,
        List<ReservaRespuesta> reservas,
        List<MultaRespuesta> multas) {
}
