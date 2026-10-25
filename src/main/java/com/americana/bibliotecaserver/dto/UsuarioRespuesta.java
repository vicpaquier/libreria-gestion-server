package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.entity.EstadoUsuario;

import java.time.OffsetDateTime;

public record UsuarioRespuesta(
        Long id,
        String nombre,
        String apellido,
        String cedula,
        String email,
        String telefono,
        String direccion,
        OffsetDateTime fechaRegistro,
        EstadoUsuario estado) {
}
