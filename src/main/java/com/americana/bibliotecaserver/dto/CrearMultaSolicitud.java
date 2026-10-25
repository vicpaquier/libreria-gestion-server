package com.americana.bibliotecaserver.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CrearMultaSolicitud(
        @NotNull @Positive Long prestamoId,
        @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal monto,
        @NotBlank String motivo) {
}
