package com.americana.bibliotecaserver.dto;

import java.util.List;

public record PaginaUsuariosRespuesta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<UsuarioRespuesta> content) {
}
