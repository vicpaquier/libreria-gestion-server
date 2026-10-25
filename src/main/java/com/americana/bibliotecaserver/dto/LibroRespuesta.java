package com.americana.bibliotecaserver.dto;

import java.util.List;

public record LibroRespuesta(
        Long id,
        String titulo,
        String isbn,
        Integer anioPublicacion,
        Integer numeroPaginas,
        int cantidadTotal,
        int cantidadDisponible,
        EditorialRespuesta editorial,
        List<AutorRespuesta> autores,
        List<CategoriaRespuesta> categorias) {
}
