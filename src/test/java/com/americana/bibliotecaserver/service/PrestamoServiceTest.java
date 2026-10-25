package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CrearPrestamoSolicitud;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.entity.Libro;
import com.americana.bibliotecaserver.entity.Usuario;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.mapper.PrestamoMapper;
import com.americana.bibliotecaserver.mapper.MultaMapper;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.MultaRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.entity.Multa;
import com.americana.bibliotecaserver.entity.Prestamo;
import org.springframework.data.domain.PageImpl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.entity.EstadoDetallePrestamo;

class PrestamoServiceTest {

    private PrestamoRepository prestamoRepository;
    private UsuarioRepository usuarioRepository;
    private LibroRepository libroRepository;
    private MultaRepository multaRepository;
    private MultaMapper multaMapper;
    private PrestamoMapper prestamoMapper;
    private PrestamoService prestamoService;

    @BeforeEach
    void configurar() {
        prestamoRepository = mock(PrestamoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        libroRepository = mock(LibroRepository.class);
        multaRepository = mock(MultaRepository.class);
        multaMapper = mock(MultaMapper.class);
        prestamoMapper = mock(PrestamoMapper.class);
        prestamoService = new PrestamoService(prestamoRepository, usuarioRepository,
                libroRepository, prestamoMapper, multaRepository, multaMapper, Clock.fixed(
                        Instant.parse("2026-09-08T15:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void rechazaUsuarioInactivoAntesDeBloquearLibros() {
        var usuario = mock(Usuario.class);
        when(usuario.getEstado()).thenReturn(EstadoUsuario.INACTIVO);
        when(usuarioRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> prestamoService.crear(solicitud(List.of(2L))))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("activo");
        verify(libroRepository, never()).buscarTodosPorIdConBloqueo(List.of(2L));
    }

    @Test
    void bloqueaUsuarioYDespuesLibrosOrdenadosYNoMutaSiUnoNoEstaDisponible() {
        var usuario = mock(Usuario.class);
        var disponible = mock(Libro.class);
        var agotado = mock(Libro.class);
        when(usuario.getEstado()).thenReturn(EstadoUsuario.ACTIVO);
        when(disponible.getCantidadDisponible()).thenReturn(1);
        when(agotado.getCantidadDisponible()).thenReturn(0);
        when(usuarioRepository.buscarPorIdConBloqueo(1L)).thenReturn(Optional.of(usuario));
        when(libroRepository.buscarTodosPorIdConBloqueo(List.of(2L, 5L)))
                .thenReturn(List.of(disponible, agotado));

        assertThatThrownBy(() -> prestamoService.crear(solicitud(List.of(5L, 2L))))
                .isInstanceOf(ConflictoNegocioException.class);

        var orden = inOrder(usuarioRepository, libroRepository);
        orden.verify(usuarioRepository).buscarPorIdConBloqueo(1L);
        orden.verify(libroRepository).buscarTodosPorIdConBloqueo(List.of(2L, 5L));
        verify(disponible, never()).prestarUnidad();
        verify(agotado, never()).prestarUnidad();
        verify(prestamoRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void detectarAtrasadosDelegaActualizacionIdempotenteConFechaActual() {
        when(prestamoRepository.marcarAtrasados(LocalDate.of(2026, 9, 8),
                EstadoPrestamo.ACTIVO, EstadoPrestamo.ATRASADO,
                EstadoDetallePrestamo.PRESTADO)).thenReturn(2);

        assertThat(prestamoService.detectarAtrasados()).isEqualTo(2);
        assertThat(prestamoService.detectarAtrasados()).isEqualTo(2);
        verify(prestamoRepository, org.mockito.Mockito.times(2)).marcarAtrasados(
                LocalDate.of(2026, 9, 8), EstadoPrestamo.ACTIVO,
                EstadoPrestamo.ATRASADO, EstadoDetallePrestamo.PRESTADO);
    }

    @Test
    void listadoCargaMultasEnUnSoloBatchYLasAsociaPorPrestamo() {
        var primero = mock(Prestamo.class);
        var segundo = mock(Prestamo.class);
        var multa = mock(Multa.class);
        var multaRespuesta = new MultaRespuesta(2L, new BigDecimal("1000.00"), "Atraso", 8L,
                OffsetDateTime.parse("2026-09-08T15:00:00Z"), EstadoMulta.PENDIENTE);
        when(primero.getId()).thenReturn(1L);
        when(segundo.getId()).thenReturn(2L);
        when(prestamoRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(primero, segundo)));
        when(prestamoRepository.buscarTodosConDetalles(List.of(1L, 2L)))
                .thenReturn(List.of(primero, segundo));
        when(multaRepository.findByPrestamoIdInOrderByPrestamoIdAscIdAsc(List.of(1L, 2L)))
                .thenReturn(List.of(multa));
        when(multaMapper.aRespuesta(multa)).thenReturn(multaRespuesta);

        prestamoService.listar(0, 20, null);

        verify(multaRepository).findByPrestamoIdInOrderByPrestamoIdAscIdAsc(List.of(1L, 2L));
        verify(prestamoMapper).aRespuesta(primero, List.of());
        verify(prestamoMapper).aRespuesta(segundo, List.of(multaRespuesta));
    }

    private CrearPrestamoSolicitud solicitud(List<Long> ids) {
        return new CrearPrestamoSolicitud(1L, LocalDate.of(2026, 9, 20), ids);
    }
}
