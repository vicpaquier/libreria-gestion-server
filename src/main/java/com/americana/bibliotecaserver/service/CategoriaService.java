package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CategoriaRespuesta;
import com.americana.bibliotecaserver.dto.CategoriaSolicitud;
import com.americana.bibliotecaserver.entity.Categoria;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.CategoriaMapper;
import com.americana.bibliotecaserver.repository.CategoriaRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private static final String RESTRICCION_NOMBRE = "uq_categorias_nombre";

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    public CategoriaService(CategoriaRepository categoriaRepository, CategoriaMapper categoriaMapper) {
        this.categoriaRepository = categoriaRepository;
        this.categoriaMapper = categoriaMapper;
    }

    @Transactional
    public CategoriaRespuesta crear(CategoriaSolicitud solicitud) {
        var nombre = solicitud.nombre().trim();
        if (categoriaRepository.existsByNombre(nombre)) {
            throw conflictoNombre();
        }

        try {
            var categoria = categoriaMapper.aEntidad(
                    nombre, normalizarOpcional(solicitud.descripcion()));
            return categoriaMapper.aRespuesta(categoriaRepository.saveAndFlush(categoria));
        } catch (DataIntegrityViolationException exception) {
            throwTraducida(exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<CategoriaRespuesta> listar() {
        return categoriaRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(categoriaMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public CategoriaRespuesta actualizar(long id, CategoriaSolicitud solicitud) {
        var categoria = buscarPorId(id);
        var nombre = solicitud.nombre().trim();
        if (categoriaRepository.existsByNombreAndIdNot(nombre, id)) {
            throw conflictoNombre();
        }

        categoria.actualizar(nombre, normalizarOpcional(solicitud.descripcion()));
        try {
            return categoriaMapper.aRespuesta(categoriaRepository.saveAndFlush(categoria));
        } catch (DataIntegrityViolationException exception) {
            throwTraducida(exception);
            throw exception;
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw categoriaNoEncontrada();
        }
    }

    @Transactional
    public void eliminar(long id) {
        var categoria = buscarPorId(id);
        try {
            categoriaRepository.delete(categoria);
            categoriaRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw categoriaNoEncontrada();
        }
    }

    private Categoria buscarPorId(long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(this::categoriaNoEncontrada);
    }

    private RecursoNoEncontradoException categoriaNoEncontrada() {
        return new RecursoNoEncontradoException("Categoría no encontrada");
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
                "NOMBRE_CATEGORIA_DUPLICADO", "nombre",
                "El nombre de la categoría ya está registrado");
    }
}
