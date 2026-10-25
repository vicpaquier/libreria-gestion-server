package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.CrearUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.UsuarioRespuesta;
import com.americana.bibliotecaserver.entity.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario aEntidad(CrearUsuarioSolicitud solicitud) {
        return new Usuario(
                solicitud.nombre(),
                solicitud.apellido(),
                solicitud.cedula(),
                solicitud.email(),
                solicitud.telefono(),
                solicitud.direccion());
    }

    public UsuarioRespuesta aRespuesta(Usuario usuario) {
        return new UsuarioRespuesta(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCedula(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getDireccion(),
                usuario.getFechaRegistro(),
                usuario.getEstado());
    }
}
