package com.americana.bibliotecaserver.exception;

import java.time.OffsetDateTime;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String codigo,
        String mensaje,
        String campo,
        String path) {
}
