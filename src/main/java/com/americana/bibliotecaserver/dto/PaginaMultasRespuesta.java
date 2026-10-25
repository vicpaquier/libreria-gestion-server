package com.americana.bibliotecaserver.dto;

import java.util.List;

public record PaginaMultasRespuesta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<MultaRespuesta> content) {
}
