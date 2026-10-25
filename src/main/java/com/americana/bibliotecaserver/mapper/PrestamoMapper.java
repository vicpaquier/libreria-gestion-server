package com.americana.bibliotecaserver.mapper;

import com.americana.bibliotecaserver.dto.DetallePrestamoRespuesta;
import com.americana.bibliotecaserver.dto.LibroResumenRespuesta;
import com.americana.bibliotecaserver.dto.PrestamoRespuesta;
import com.americana.bibliotecaserver.dto.PrestamoResumenRespuesta;
import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.entity.DetallePrestamo;
import com.americana.bibliotecaserver.entity.Prestamo;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PrestamoMapper {

    private final Clock clock;

    public PrestamoMapper(Clock clock) {
        this.clock = clock;
    }

    public PrestamoRespuesta aRespuesta(Prestamo prestamo, List<MultaRespuesta> multas) {
        return new PrestamoRespuesta(
                prestamo.getId(),
                prestamo.getUsuario().getId(),
                prestamo.getFechaPrestamo(),
                prestamo.getFechaVencimiento(),
                prestamo.getEstado(),
                diasAtraso(prestamo),
                prestamo.getDetalles().stream().map(this::aDetalle).toList(),
                multas);
    }

    public PrestamoResumenRespuesta aResumen(Prestamo prestamo) {
        return new PrestamoResumenRespuesta(
                prestamo.getId(),
                prestamo.getUsuario().getId(),
                prestamo.getFechaPrestamo(),
                prestamo.getFechaVencimiento(),
                prestamo.getEstado(),
                diasAtraso(prestamo));
    }

    private DetallePrestamoRespuesta aDetalle(DetallePrestamo detalle) {
        var libro = detalle.getLibro();
        return new DetallePrestamoRespuesta(
                detalle.getId(),
                new LibroResumenRespuesta(libro.getId(), libro.getTitulo(), libro.getIsbn()),
                detalle.getFechaDevolucion(),
                detalle.getEstado());
    }

    private Integer diasAtraso(Prestamo prestamo) {
        if (prestamo.getEstado() != com.americana.bibliotecaserver.entity.EstadoPrestamo.ATRASADO) {
            return null;
        }
        return Math.max(0, Math.toIntExact(ChronoUnit.DAYS.between(
                prestamo.getFechaVencimiento(), LocalDate.now(clock))));
    }
}
