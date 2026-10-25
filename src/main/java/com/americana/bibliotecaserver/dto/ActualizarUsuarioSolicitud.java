package com.americana.bibliotecaserver.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ActualizarUsuarioSolicitud(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "El apellido es obligatorio") String apellido,
        @Email(message = "El email no tiene un formato válido") String email,
        String telefono,
        String direccion) {

    public ActualizarUsuarioSolicitud {
        email = email == null || email.isBlank() ? null : email;
    }

    @JsonAnySetter
    public void rechazarCampoNoEditable(String campo, Object valor) {
        throw new IllegalArgumentException("El campo no es editable: " + campo);
    }
}
