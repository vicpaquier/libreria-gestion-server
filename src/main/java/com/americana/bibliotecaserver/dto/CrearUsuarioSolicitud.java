package com.americana.bibliotecaserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CrearUsuarioSolicitud(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El apellido es obligatorio") String apellido,
        @NotBlank(message = "La cédula es obligatoria") String cedula,
        @Email(message = "El email no tiene un formato válido") String email,
        String telefono,
        String direccion) {

    public CrearUsuarioSolicitud {
        email = email == null || email.isBlank() ? null : email;
    }
}
