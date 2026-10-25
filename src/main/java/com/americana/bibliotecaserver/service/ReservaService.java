package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CrearReservaSolicitud;
import com.americana.bibliotecaserver.dto.ReservaRespuesta;
import com.americana.bibliotecaserver.entity.EstadoReserva;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.entity.Reserva;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.ReservaMapper;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.ReservaRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final ReservaMapper reservaMapper;
    private final Clock clock;

    public ReservaService(ReservaRepository reservaRepository, UsuarioRepository usuarioRepository,
            LibroRepository libroRepository, ReservaMapper reservaMapper, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.libroRepository = libroRepository;
        this.reservaMapper = reservaMapper;
        this.clock = clock;
    }

    @Transactional
    public ReservaRespuesta crear(CrearReservaSolicitud solicitud) {
        var usuario = usuarioRepository.buscarPorIdConBloqueo(solicitud.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ConflictoNegocioException("USUARIO_INACTIVO", "usuarioId",
                    "El usuario debe estar activo para realizar una reserva");
        }
        var libro = libroRepository.findById(solicitud.libroId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado"));
        if (reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(
                solicitud.usuarioId(), solicitud.libroId(), EstadoReserva.PENDIENTE)) {
            throw reservaDuplicada();
        }
        try {
            var reserva = new Reserva(usuario, libro, OffsetDateTime.now(clock), null);
            return reservaMapper.aRespuesta(reservaRepository.saveAndFlush(reserva));
        } catch (DataIntegrityViolationException exception) {
            var detalle = exception.getMostSpecificCause().getMessage();
            if (detalle != null && detalle.contains("uq_reservas_pendiente_usuario_libro")) {
                throw reservaDuplicada();
            }
            throw exception;
        }
    }

    @Transactional
    public ReservaRespuesta atender(long id) {
        var reserva = buscarPendiente(id);
        reserva.atender();
        return reservaMapper.aRespuesta(reservaRepository.saveAndFlush(reserva));
    }

    @Transactional
    public ReservaRespuesta cancelar(long id) {
        var reserva = buscarPendiente(id);
        reserva.cancelar();
        return reservaMapper.aRespuesta(reservaRepository.saveAndFlush(reserva));
    }

    @Transactional
    public int expirarVencidas() {
        return reservaRepository.marcarExpiradas(OffsetDateTime.now(clock),
                EstadoReserva.PENDIENTE, EstadoReserva.EXPIRADA);
    }

    private Reserva buscarPendiente(long id) {
        var reserva = reservaRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada"));
        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new ConflictoNegocioException("RESERVA_NO_PENDIENTE", "id",
                    "Sólo una reserva pendiente puede cambiar de estado");
        }
        return reserva;
    }

    private ConflictoNegocioException reservaDuplicada() {
        return new ConflictoNegocioException("RESERVA_PENDIENTE_DUPLICADA", "libroId",
                "El usuario ya tiene una reserva pendiente para el libro");
    }
}
