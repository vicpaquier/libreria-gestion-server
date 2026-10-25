package com.americana.bibliotecaserver.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoriaSolicitud(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String descripcion) {
}
