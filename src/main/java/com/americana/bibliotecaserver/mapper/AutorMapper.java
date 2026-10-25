package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.AutorRespuesta;
import com.americana.bibliotecaserver.dto.AutorSolicitud;
import com.americana.bibliotecaserver.entity.Autor;
import org.springframework.stereotype.Component;

@Component
public class AutorMapper {

    public Autor aEntidad(AutorSolicitud solicitud) {
        return new Autor(solicitud.nombre(), solicitud.apellido(), solicitud.nacionalidad());
    }

    public AutorRespuesta aRespuesta(Autor autor) {
        return new AutorRespuesta(
                autor.getId(), autor.getNombre(), autor.getApellido(), autor.getNacionalidad());
    }
}
