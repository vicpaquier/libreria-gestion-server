package com.americana.bibliotecaserver.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AjustarStockLibroSolicitud(
        @NotNull(message = "La cantidad total es obligatoria")
        @Min(value = 0, message = "La cantidad total no puede ser negativa") Integer cantidadTotal) {
}
