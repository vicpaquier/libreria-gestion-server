package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.LibroDetalleRespuesta;
import com.americana.bibliotecaserver.dto.LibroRespuesta;
import com.americana.bibliotecaserver.dto.PrestamoResumenRespuesta;
import com.americana.bibliotecaserver.dto.ReservaRespuesta;
import com.americana.bibliotecaserver.entity.Libro;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LibroMapper {

    private final AutorMapper autorMapper;
    private final CategoriaMapper categoriaMapper;
    private final EditorialMapper editorialMapper;

    public LibroMapper(AutorMapper autorMapper, CategoriaMapper categoriaMapper,
            EditorialMapper editorialMapper) {
        this.autorMapper = autorMapper;
        this.categoriaMapper = categoriaMapper;
        this.editorialMapper = editorialMapper;
    }

    public LibroRespuesta aRespuesta(Libro libro) {
        return new LibroRespuesta(
                libro.getId(),
                libro.getTitulo(),
                libro.getIsbn(),
                libro.getAnioPublicacion(),
                libro.getNumeroPaginas(),
                libro.getCantidadTotal(),
                libro.getCantidadDisponible(),
                libro.getEditorial() == null ? null : editorialMapper.aRespuesta(libro.getEditorial()),
                libro.getAutores().stream().map(autorMapper::aRespuesta).toList(),
                libro.getCategorias().stream().map(categoriaMapper::aRespuesta).toList());
    }

    public LibroDetalleRespuesta aDetalle(
            Libro libro, List<PrestamoResumenRespuesta> prestamosActivos,
            List<ReservaRespuesta> reservasPendientes) {
        var respuesta = aRespuesta(libro);
        return new LibroDetalleRespuesta(
                respuesta.id(), respuesta.titulo(), respuesta.isbn(), respuesta.anioPublicacion(),
                respuesta.numeroPaginas(), respuesta.cantidadTotal(), respuesta.cantidadDisponible(),
                respuesta.editorial(), respuesta.autores(), respuesta.categorias(),
                prestamosActivos, reservasPendientes);
    }
}
