package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.CategoriaRespuesta;
import com.americana.bibliotecaserver.entity.Categoria;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

    public Categoria aEntidad(String nombre, String descripcion) {
        return new Categoria(nombre, descripcion);
    }

    public CategoriaRespuesta aRespuesta(Categoria categoria) {
        return new CategoriaRespuesta(
                categoria.getId(), categoria.getNombre(), categoria.getDescripcion());
    }
}
