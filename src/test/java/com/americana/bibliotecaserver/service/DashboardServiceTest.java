package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.entity.EstadoReserva;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.MultaRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.repository.ReservaRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @Test
    void agregaTodosLosIndicadoresDelContrato() {
        var libros = mock(LibroRepository.class);
        var prestamos = mock(PrestamoRepository.class);
        var reservas = mock(ReservaRepository.class);
        var usuarios = mock(UsuarioRepository.class);
        var multas = mock(MultaRepository.class);
        when(libros.count()).thenReturn(10L);
        when(libros.sumarCantidadDisponible()).thenReturn(24L);
        when(prestamos.countByEstado(EstadoPrestamo.ACTIVO)).thenReturn(3L);
        when(prestamos.countByEstado(EstadoPrestamo.ATRASADO)).thenReturn(2L);
        when(reservas.countByEstado(EstadoReserva.PENDIENTE)).thenReturn(4L);
        when(usuarios.countByEstado(EstadoUsuario.ACTIVO)).thenReturn(8L);
        when(multas.sumarMontoPorEstado(EstadoMulta.PENDIENTE))
                .thenReturn(new BigDecimal("75000.50"));

        var resumen = new DashboardService(libros, prestamos, reservas, usuarios, multas)
                .consultar();

        assertThat(resumen.libros()).isEqualTo(10);
        assertThat(resumen.ejemplaresDisponibles()).isEqualTo(24);
        assertThat(resumen.prestamosActivos()).isEqualTo(3);
        assertThat(resumen.prestamosAtrasados()).isEqualTo(2);
        assertThat(resumen.reservasPendientes()).isEqualTo(4);
        assertThat(resumen.usuariosActivos()).isEqualTo(8);
        assertThat(resumen.multasPendientes()).isEqualByComparingTo("75000.50");
    }
}
