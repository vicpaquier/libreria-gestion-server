package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.EditorialRespuesta;
import com.americana.bibliotecaserver.entity.Editorial;
import org.springframework.stereotype.Component;

@Component
public class EditorialMapper {

    public Editorial aEntidad(String nombre, String pais) {
        return new Editorial(nombre, pais);
    }

    public EditorialRespuesta aRespuesta(Editorial editorial) {
        return new EditorialRespuesta(
                editorial.getId(), editorial.getNombre(), editorial.getPais());
    }
}
