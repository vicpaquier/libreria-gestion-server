package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.EditorialRespuesta;
import com.americana.bibliotecaserver.dto.EditorialSolicitud;
import com.americana.bibliotecaserver.entity.Editorial;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.EditorialMapper;
import com.americana.bibliotecaserver.repository.EditorialRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EditorialService {

    private static final String RESTRICCION_NOMBRE = "uq_editoriales_nombre";

    private final EditorialRepository editorialRepository;
    private final EditorialMapper editorialMapper;

    public EditorialService(EditorialRepository editorialRepository, EditorialMapper editorialMapper) {
        this.editorialRepository = editorialRepository;
        this.editorialMapper = editorialMapper;
    }

    @Transactional
    public EditorialRespuesta crear(EditorialSolicitud solicitud) {
        var nombre = solicitud.nombre().trim();
        if (editorialRepository.existsByNombre(nombre)) {
            throw conflictoNombre();
        }

        try {
            var editorial = editorialMapper.aEntidad(nombre, normalizarOpcional(solicitud.pais()));
            return editorialMapper.aRespuesta(editorialRepository.saveAndFlush(editorial));
        } catch (DataIntegrityViolationException exception) {
            throwTraducida(exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<EditorialRespuesta> listar() {
        return editorialRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(editorialMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public EditorialRespuesta actualizar(long id, EditorialSolicitud solicitud) {
        var editorial = buscarPorId(id);
        var nombre = solicitud.nombre().trim();
        if (editorialRepository.existsByNombreAndIdNot(nombre, id)) {
            throw conflictoNombre();
        }

        editorial.actualizar(nombre, normalizarOpcional(solicitud.pais()));
        try {
            return editorialMapper.aRespuesta(editorialRepository.saveAndFlush(editorial));
        } catch (DataIntegrityViolationException exception) {
            throwTraducida(exception);
            throw exception;
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw editorialNoEncontrada();
        }
    }

    @Transactional
    public void eliminar(long id) {
        var editorial = buscarPorId(id);
        try {
            editorialRepository.delete(editorial);
            editorialRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw editorialNoEncontrada();
        }
    }

    private Editorial buscarPorId(long id) {
        return editorialRepository.findById(id)
                .orElseThrow(this::editorialNoEncontrada);
    }

    private RecursoNoEncontradoException editorialNoEncontrada() {
        return new RecursoNoEncontradoException("Editorial no encontrada");
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return valor.trim();
    }

    private void throwTraducida(DataIntegrityViolationException exception) {
        Throwable causa = exception;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException violacion
                    && RESTRICCION_NOMBRE.equals(violacion.getConstraintName())) {
                throw conflictoNombre();
            }
            causa = causa.getCause();
        }

        var detalle = exception.getMostSpecificCause().getMessage();
        if (detalle != null && detalle.contains(RESTRICCION_NOMBRE)) {
            throw conflictoNombre();
        }
    }

    private ConflictoUnicidadException conflictoNombre() {
        return new ConflictoUnicidadException(
                "NOMBRE_EDITORIAL_DUPLICADO", "nombre", "El nombre de la editorial ya está registrado");
    }
}
