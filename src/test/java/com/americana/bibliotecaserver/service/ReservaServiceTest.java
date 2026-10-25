package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CrearReservaSolicitud;
import com.americana.bibliotecaserver.entity.EstadoReserva;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.entity.Libro;
import com.americana.bibliotecaserver.entity.Reserva;
import com.americana.bibliotecaserver.entity.Usuario;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.mapper.ReservaMapper;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.ReservaRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservaServiceTest {

    private ReservaRepository reservaRepository;
    private UsuarioRepository usuarioRepository;
    private LibroRepository libroRepository;
    private ReservaService reservaService;

    @BeforeEach
    void configurar() {
        reservaRepository = mock(ReservaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        libroRepository = mock(LibroRepository.class);
        reservaService = new ReservaService(reservaRepository, usuarioRepository, libroRepository,
                mock(ReservaMapper.class), Clock.fixed(
                        Instant.parse("2026-09-08T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void noExigeStockCeroYDejaExpiracionNula() {
        var usuario = mock(Usuario.class);
        var libro = mock(Libro.class);
        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        when(usuarioRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(libroRepository.findById(2L)).thenReturn(Optional.of(libro));
        when(reservaRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        reservaService.crear(new CrearReservaSolicitud(1L, 2L));

        var captor = org.mockito.ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getFechaExpiracion()).isNull();
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        verify(libro, never()).getCantidadDisponible();
    }

    @Test
    void rechazaUsuarioInactivoYReservaPendienteDuplicada() {
        var inactivo = mock(Usuario.class);
        when(inactivo.getEstado()).thenReturn(EstadoUsuario.INACTIVO);
        when(usuarioRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(inactivo));
        assertThatThrownBy(() -> reservaService.crear(new CrearReservaSolicitud(1L, 2L)))
                .isInstanceOf(ConflictoNegocioException.class);

        var activo = mock(Usuario.class);
        when(activo.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        when(usuarioRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(activo));
        when(libroRepository.findById(2L)).thenReturn(Optional.of(mock(Libro.class)));
        when(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(
                1L, 2L, EstadoReserva.PENDIENTE)).thenReturn(true);
        assertThatThrownBy(() -> reservaService.crear(new CrearReservaSolicitud(1L, 2L)))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("ya tiene");
    }

    @Test
    void soloPermiteTransicionDesdePendiente() {
        var reserva = mock(Reserva.class);
        when(reserva.getEstado()).thenReturn(EstadoReserva.ATENDIDA);
        when(reservaRepository.buscarPorIdConBloqueo(4L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelar(4L))
                .isInstanceOf(ConflictoNegocioException.class);
        verify(reserva, never()).cancelar();
    }

    @Test
    void expiraVencidasConActualizacionIdempotente() {
        var ahora = java.time.OffsetDateTime.parse("2026-09-08T15:00:00Z");
        when(reservaRepository.marcarExpiradas(
                ahora, EstadoReserva.PENDIENTE, EstadoReserva.EXPIRADA))
                .thenReturn(2, 0);

        assertThat(reservaService.expirarVencidas()).isEqualTo(2);
        assertThat(reservaService.expirarVencidas()).isZero();
        verify(reservaRepository, org.mockito.Mockito.times(2)).marcarExpiradas(
                ahora, EstadoReserva.PENDIENTE, EstadoReserva.EXPIRADA);
    }
}
