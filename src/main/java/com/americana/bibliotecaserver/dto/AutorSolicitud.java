package com.americana.bibliotecaserver.dto;

import jakarta.validation.constraints.NotBlank;

public record AutorSolicitud(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El apellido es obligatorio") String apellido,
        String nacionalidad) {
}
