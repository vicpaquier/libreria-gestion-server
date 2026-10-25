package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.entity.Multa;
import org.springframework.stereotype.Component;

@Component
public class MultaMapper {

    public MultaRespuesta aRespuesta(Multa multa) {
        return new MultaRespuesta(
                multa.getPrestamo().getId(),
                multa.getMonto(),
                multa.getMotivo(),
                multa.getId(),
                multa.getFechaMulta(),
                multa.getEstado());
    }
}
