package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.HistorialUsuarioRespuesta;
import com.americana.bibliotecaserver.dto.UsuarioDetalleRespuesta;
import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.MultaMapper;
import com.americana.bibliotecaserver.mapper.PrestamoMapper;
import com.americana.bibliotecaserver.mapper.ReservaMapper;
import com.americana.bibliotecaserver.repository.MultaRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.repository.ReservaRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HistorialUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final MultaRepository multaRepository;
    private final PrestamoMapper prestamoMapper;
    private final ReservaMapper reservaMapper;
    private final MultaMapper multaMapper;

    public HistorialUsuarioService(UsuarioRepository usuarioRepository,
            PrestamoRepository prestamoRepository, ReservaRepository reservaRepository,
            MultaRepository multaRepository, PrestamoMapper prestamoMapper,
            ReservaMapper reservaMapper, MultaMapper multaMapper) {
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
        this.multaRepository = multaRepository;
        this.prestamoMapper = prestamoMapper;
        this.reservaMapper = reservaMapper;
        this.multaMapper = multaMapper;
    }

    @Transactional(readOnly = true)
    public HistorialUsuarioRespuesta consultarHistorial(long usuarioId) {
        validarUsuario(usuarioId);
        return cargarHistorial(usuarioId);
    }

    @Transactional(readOnly = true)
    public UsuarioDetalleRespuesta consultarDetalle(long usuarioId) {
        var usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        var historial = cargarHistorial(usuarioId);
        return new UsuarioDetalleRespuesta(
                usuario.getId(), usuario.getNombre(), usuario.getApellido(), usuario.getCedula(),
                usuario.getEmail(), usuario.getTelefono(), usuario.getDireccion(),
                usuario.getFechaRegistro(), usuario.getEstado(), historial.prestamos(),
                historial.reservas(), historial.multas());
    }

    private HistorialUsuarioRespuesta cargarHistorial(long usuarioId) {
        var multas = multaRepository.findByPrestamoUsuarioIdOrderByIdAsc(usuarioId).stream()
                .map(multaMapper::aRespuesta).toList();
        Map<Long, List<MultaRespuesta>> multasPorPrestamo =
                multas.stream().collect(Collectors.groupingBy(
                        MultaRespuesta::prestamoId));
        var prestamos = prestamoRepository.buscarPorUsuarioConDetalles(usuarioId).stream()
                .map(prestamo -> prestamoMapper.aRespuesta(prestamo,
                        multasPorPrestamo.getOrDefault(prestamo.getId(), List.of())))
                .toList();
        var reservas = reservaRepository.findByUsuarioIdOrderByIdAsc(usuarioId).stream()
                .map(reservaMapper::aRespuesta).toList();
        return new HistorialUsuarioRespuesta(prestamos, reservas, multas);
    }

    private void validarUsuario(long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado");
        }
    }
}
