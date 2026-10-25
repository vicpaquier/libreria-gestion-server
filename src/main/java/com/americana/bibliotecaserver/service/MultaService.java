package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CrearMultaSolicitud;
import com.americana.bibliotecaserver.dto.MultaRespuesta;
import com.americana.bibliotecaserver.dto.PaginaMultasRespuesta;
import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.entity.Multa;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.MultaMapper;
import com.americana.bibliotecaserver.repository.MultaRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;

@Service
public class MultaService {

    private final MultaRepository multaRepository;
    private final PrestamoRepository prestamoRepository;
    private final MultaMapper multaMapper;
    private final Clock clock;

    public MultaService(MultaRepository multaRepository, PrestamoRepository prestamoRepository,
            MultaMapper multaMapper, Clock clock) {
        this.multaRepository = multaRepository;
        this.prestamoRepository = prestamoRepository;
        this.multaMapper = multaMapper;
        this.clock = clock;
    }

    @Transactional
    public MultaRespuesta crear(CrearMultaSolicitud solicitud) {
        var prestamo = prestamoRepository.findById(solicitud.prestamoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo no encontrado"));
        var multa = new Multa(prestamo, solicitud.monto(), solicitud.motivo().trim(),
                OffsetDateTime.now(clock));
        return multaMapper.aRespuesta(multaRepository.saveAndFlush(multa));
    }

    @Transactional(readOnly = true)
    public PaginaMultasRespuesta listar(int page, int size, Long usuarioId, Long prestamoId,
            EstadoMulta estado, LocalDate fecha) {
        Specification<Multa> filtros = (root, query, cb) -> {
            var predicados = new ArrayList<Predicate>();
            if (usuarioId != null) {
                predicados.add(cb.equal(root.get("prestamo").get("usuario").get("id"), usuarioId));
            }
            if (prestamoId != null) {
                predicados.add(cb.equal(root.get("prestamo").get("id"), prestamoId));
            }
            if (estado != null) predicados.add(cb.equal(root.get("estado"), estado));
            if (fecha != null) {
                var inicio = fecha.atStartOfDay(clock.getZone()).toOffsetDateTime();
                predicados.add(cb.greaterThanOrEqualTo(root.get("fechaMulta"), inicio));
                predicados.add(cb.lessThan(root.get("fechaMulta"), inicio.plusDays(1)));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
        var pagina = multaRepository.findAll(filtros,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PaginaMultasRespuesta(page, size, pagina.getTotalElements(),
                pagina.getTotalPages(), pagina.getContent().stream()
                        .map(multaMapper::aRespuesta).toList());
    }

    @Transactional
    public MultaRespuesta pagar(long id) {
        var multa = buscarPendiente(id);
        multa.pagar();
        return multaMapper.aRespuesta(multaRepository.saveAndFlush(multa));
    }

    @Transactional
    public MultaRespuesta cancelar(long id) {
        var multa = buscarPendiente(id);
        multa.cancelar();
        return multaMapper.aRespuesta(multaRepository.saveAndFlush(multa));
    }

    private Multa buscarPendiente(long id) {
        var multa = multaRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Multa no encontrada"));
        if (multa.getEstado() != EstadoMulta.PENDIENTE) {
            throw new ConflictoNegocioException("MULTA_NO_PENDIENTE", "id",
                    "Sólo una multa pendiente puede cambiar de estado");
        }
        return multa;
    }
}
