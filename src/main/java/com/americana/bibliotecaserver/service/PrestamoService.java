package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CrearPrestamoSolicitud;
import com.americana.bibliotecaserver.dto.PaginaPrestamosRespuesta;
import com.americana.bibliotecaserver.dto.PrestamoRespuesta;
import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.entity.DetallePrestamo;
import com.americana.bibliotecaserver.entity.EstadoDetallePrestamo;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.entity.Prestamo;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.PrestamoMapper;
import com.americana.bibliotecaserver.mapper.MultaMapper;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import com.americana.bibliotecaserver.repository.MultaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Service
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final PrestamoMapper prestamoMapper;
    private final MultaRepository multaRepository;
    private final MultaMapper multaMapper;
    private final Clock clock;

    public PrestamoService(PrestamoRepository prestamoRepository,
            UsuarioRepository usuarioRepository, LibroRepository libroRepository,
            PrestamoMapper prestamoMapper, MultaRepository multaRepository,
            MultaMapper multaMapper, Clock clock) {
        this.prestamoRepository = prestamoRepository;
        this.usuarioRepository = usuarioRepository;
        this.libroRepository = libroRepository;
        this.prestamoMapper = prestamoMapper;
        this.multaRepository = multaRepository;
        this.multaMapper = multaMapper;
        this.clock = clock;
    }

    @Transactional
    public PrestamoRespuesta crear(CrearPrestamoSolicitud solicitud) {
        var usuario = usuarioRepository.buscarPorIdConBloqueo(solicitud.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new ConflictoNegocioException("USUARIO_INACTIVO", "usuarioId",
                    "El usuario debe estar activo para solicitar un préstamo");
        }

        var idsOrdenados = solicitud.librosIds().stream().sorted().toList();
        var libros = libroRepository.buscarTodosPorIdConBloqueo(idsOrdenados);
        if (libros.size() != idsOrdenados.size()) {
            throw new RecursoNoEncontradoException("Uno o más libros no existen");
        }
        if (libros.stream().anyMatch(libro -> libro.getCantidadDisponible() < 1)) {
            throw new ConflictoNegocioException("LIBRO_NO_DISPONIBLE", "librosIds",
                    "Uno o más libros no tienen unidades disponibles");
        }

        libros.forEach(libro -> libro.prestarUnidad());
        var prestamo = new Prestamo(usuario, OffsetDateTime.now(clock),
                solicitud.fechaVencimiento(), libros);
        return prestamoMapper.aRespuesta(prestamoRepository.saveAndFlush(prestamo), List.of());
    }

    @Transactional(readOnly = true)
    public PaginaPrestamosRespuesta listar(int page, int size, EstadoPrestamo estado) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Prestamo> pagina = estado == null
                ? prestamoRepository.findAll(pageable)
                : prestamoRepository.findByEstado(estado, pageable);
        var ids = pagina.getContent().stream().map(Prestamo::getId).toList();
        var multas = buscarMultasPorPrestamo(ids);
        List<PrestamoRespuesta> contenido = ids.isEmpty() ? List.of()
                : prestamoRepository.buscarTodosConDetalles(ids).stream()
                        .map(prestamo -> prestamoMapper.aRespuesta(prestamo,
                                multas.getOrDefault(prestamo.getId(), List.of())))
                        .toList();
        return new PaginaPrestamosRespuesta(page, size, pagina.getTotalElements(),
                pagina.getTotalPages(), contenido);
    }

    @Transactional
    public int detectarAtrasados() {
        return prestamoRepository.marcarAtrasados(LocalDate.now(clock), EstadoPrestamo.ACTIVO,
                EstadoPrestamo.ATRASADO, EstadoDetallePrestamo.PRESTADO);
    }

    @Transactional
    public PrestamoRespuesta devolver(long idPrestamo, long idDetalle) {
        var prestamo = buscarPrestamoConBloqueo(idPrestamo);
        var detalle = buscarDetalle(prestamo, idDetalle);
        validarPrestado(detalle);
        var libro = libroRepository.buscarPorIdConBloqueo(detalle.getLibro().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado"));

        detalle.devolver(OffsetDateTime.now(clock));
        libro.devolverUnidad();
        prestamo.finalizarSiNoHayPrestados();
        prestamoRepository.flush();
        return aRespuestaConMultas(prestamo);
    }

    @Transactional
    public PrestamoRespuesta marcarPerdido(long idPrestamo, long idDetalle) {
        var prestamo = buscarPrestamoConBloqueo(idPrestamo);
        var detalle = buscarDetalle(prestamo, idDetalle);
        validarPrestado(detalle);
        detalle.marcarPerdido();
        prestamo.finalizarSiNoHayPrestados();
        prestamoRepository.flush();
        return aRespuestaConMultas(prestamo);
    }

    private Prestamo buscarPrestamoConBloqueo(long idPrestamo) {
        return prestamoRepository.buscarPorIdConBloqueo(idPrestamo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));
    }

    private DetallePrestamo buscarDetalle(Prestamo prestamo, long idDetalle) {
        return prestamo.getDetalles().stream()
                .filter(detalle -> detalle.getId().equals(idDetalle))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Detalle no encontrado en el préstamo"));
    }

    private void validarPrestado(DetallePrestamo detalle) {
        if (detalle.getEstado() != EstadoDetallePrestamo.PRESTADO) {
            throw new ConflictoNegocioException("DETALLE_FINALIZADO", "idDetalle",
                    "El detalle ya fue devuelto o marcado como perdido");
        }
    }

    private PrestamoRespuesta aRespuestaConMultas(Prestamo prestamo) {
        var multas = buscarMultasPorPrestamo(List.of(prestamo.getId()));
        return prestamoMapper.aRespuesta(prestamo,
                multas.getOrDefault(prestamo.getId(), List.of()));
    }

    private Map<Long, List<MultaRespuesta>>
            buscarMultasPorPrestamo(List<Long> prestamosIds) {
        if (prestamosIds.isEmpty()) return Map.of();
        return multaRepository.findByPrestamoIdInOrderByPrestamoIdAscIdAsc(prestamosIds).stream()
                .map(multaMapper::aRespuesta)
                .collect(Collectors.groupingBy(
                        MultaRespuesta::prestamoId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }
}
