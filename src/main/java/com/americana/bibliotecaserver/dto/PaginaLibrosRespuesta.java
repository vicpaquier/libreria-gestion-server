package com.americana.bibliotecaserver.dto;

import java.util.List;

public record PaginaLibrosRespuesta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<LibroRespuesta> content) {
}
