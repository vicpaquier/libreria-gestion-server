package com.americana.bibliotecaserver.dto;

import com.americana.bibliotecaserver.entity.EstadoUsuario;

import java.time.OffsetDateTime;
import java.util.List;

public record UsuarioDetalleRespuesta(
        Long id,
        String nombre,
        String apellido,
        String cedula,
        String email,
        String telefono,
        String direccion,
        OffsetDateTime fechaRegistro,
        EstadoUsuario estado,
        List<PrestamoRespuesta> prestamos,
        List<ReservaRespuesta> reservas,
        List<MultaRespuesta> multas) {
}
