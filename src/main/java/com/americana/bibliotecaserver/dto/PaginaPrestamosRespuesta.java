package com.americana.bibliotecaserver.dto;

import java.util.List;

public record PaginaPrestamosRespuesta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<PrestamoRespuesta> content) {
}
