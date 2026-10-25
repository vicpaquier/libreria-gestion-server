package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.ActualizarUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CambiarEstadoUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CrearUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.PaginaUsuariosRespuesta;
import com.americana.bibliotecaserver.dto.UsuarioRespuesta;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.entity.Usuario;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.UsuarioMapper;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;

@Service
public class UsuarioService {

    private static final String RESTRICCION_CEDULA = "uq_usuarios_cedula";
    private static final String RESTRICCION_EMAIL = "uq_usuarios_email";

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
    }

    @Transactional
    public UsuarioRespuesta crear(CrearUsuarioSolicitud solicitud) {
        if (usuarioRepository.existsByCedula(solicitud.cedula())) {
            throw conflictoCedula();
        }
        if (solicitud.email() != null && usuarioRepository.existsByEmail(solicitud.email())) {
            throw conflictoEmail();
        }

        try {
            var usuario = usuarioMapper.aEntidad(solicitud);
            return usuarioMapper.aRespuesta(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            var restriccion = obtenerRestriccion(exception);
            if (RESTRICCION_CEDULA.equals(restriccion)) {
                throw conflictoCedula();
            }
            if (RESTRICCION_EMAIL.equals(restriccion)) {
                throw conflictoEmail();
            }
            throw exception;
        }
    }

    @Transactional
    public UsuarioRespuesta actualizar(long id, ActualizarUsuarioSolicitud solicitud) {
        var usuario = buscarPorIdConBloqueo(id);
        if (solicitud.email() != null
                && usuarioRepository.existsByEmailAndIdNot(solicitud.email(), id)) {
            throw conflictoEmail();
        }

        usuario.actualizar(
                solicitud.nombre(),
                solicitud.apellido(),
                solicitud.email(),
                solicitud.telefono(),
                solicitud.direccion());
        return guardarActualizacion(usuario);
    }

    @Transactional
    public UsuarioRespuesta cambiarEstado(long id, CambiarEstadoUsuarioSolicitud solicitud) {
        var usuario = buscarPorIdConBloqueo(id);
        usuario.cambiarEstado(solicitud.estado());
        return guardarActualizacion(usuario);
    }

    @Transactional(readOnly = true)
    public PaginaUsuariosRespuesta listar(int page, int size, EstadoUsuario estado, String buscar) {
        var patron = crearPatronBusqueda(buscar);
        Specification<Usuario> filtros = (root, query, criteriaBuilder) -> {
            var predicados = new ArrayList<Predicate>();
            if (estado != null) {
                predicados.add(criteriaBuilder.equal(root.get("estado"), estado));
            }
            if (patron != null) {
                predicados.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), patron, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("apellido")), patron, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("cedula")), patron, '\\')));
            }
            return criteriaBuilder.and(predicados.toArray(Predicate[]::new));
        };

        var pagina = usuarioRepository.findAll(
                filtros,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        var contenido = pagina.getContent().stream().map(usuarioMapper::aRespuesta).toList();
        return new PaginaUsuariosRespuesta(
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                contenido);
    }

    private String crearPatronBusqueda(String buscar) {
        if (buscar == null || buscar.trim().isEmpty()) {
            return null;
        }
        var termino = buscar.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + termino + "%";
    }

    private Usuario buscarPorIdConBloqueo(long id) {
        return usuarioRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private UsuarioRespuesta guardarActualizacion(Usuario usuario) {
        try {
            return usuarioMapper.aRespuesta(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            if (RESTRICCION_EMAIL.equals(obtenerRestriccion(exception))) {
                throw conflictoEmail();
            }
            throw exception;
        }
    }

    private String obtenerRestriccion(DataIntegrityViolationException exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacion
                    && violacion.getConstraintName() != null) {
                return violacion.getConstraintName();
            }
            causa = causa.getCause();
        }

        var detalle = exception.getMostSpecificCause().getMessage();
        if (detalle != null && detalle.contains(RESTRICCION_CEDULA)) {
            return RESTRICCION_CEDULA;
        }
        if (detalle != null && detalle.contains(RESTRICCION_EMAIL)) {
            return RESTRICCION_EMAIL;
        }
        return null;
    }

    private ConflictoUnicidadException conflictoCedula() {
        return new ConflictoUnicidadException(
                "CEDULA_DUPLICADA", "cedula", "La cédula ya está registrada");
    }

    private ConflictoUnicidadException conflictoEmail() {
        return new ConflictoUnicidadException(
                "EMAIL_DUPLICADO", "email", "El email ya está registrado");
    }
}
