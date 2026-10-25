package com.americana.bibliotecaserver.dto;

import jakarta.validation.constraints.NotBlank;

public record EditorialSolicitud(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String pais) {
}
