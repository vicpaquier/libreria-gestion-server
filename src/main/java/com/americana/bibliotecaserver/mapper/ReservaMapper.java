package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.LibroResumenRespuesta;
import com.americana.bibliotecaserver.dto.ReservaRespuesta;
import com.americana.bibliotecaserver.entity.Reserva;
import org.springframework.stereotype.Component;

@Component
public class ReservaMapper {

    public ReservaRespuesta aRespuesta(Reserva reserva) {
        var libro = reserva.getLibro();
        return new ReservaRespuesta(
                reserva.getId(),
                reserva.getUsuario().getId(),
                new LibroResumenRespuesta(libro.getId(), libro.getTitulo(), libro.getIsbn()),
                reserva.getFechaReserva(),
                reserva.getFechaExpiracion(),
                reserva.getEstado());
    }
}
