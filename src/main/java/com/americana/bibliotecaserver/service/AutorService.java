package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.AutorRespuesta;
import com.americana.bibliotecaserver.dto.AutorSolicitud;
import com.americana.bibliotecaserver.entity.Autor;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.AutorMapper;
import com.americana.bibliotecaserver.repository.AutorRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AutorService {

    private final AutorRepository autorRepository;
    private final AutorMapper autorMapper;

    public AutorService(AutorRepository autorRepository, AutorMapper autorMapper) {
        this.autorRepository = autorRepository;
        this.autorMapper = autorMapper;
    }

    @Transactional
    public AutorRespuesta crear(AutorSolicitud solicitud) {
        return autorMapper.aRespuesta(autorRepository.save(autorMapper.aEntidad(solicitud)));
    }

    @Transactional(readOnly = true)
    public List<AutorRespuesta> listar(String buscar) {
        var patron = crearPatronBusqueda(buscar);
        var orden = Sort.by(Sort.Direction.ASC, "id");
        if (patron == null) {
            return autorRepository.findAll(orden).stream()
                    .map(autorMapper::aRespuesta)
                    .toList();
        }

        Specification<Autor> filtro = (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), patron, '\\'),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("apellido")), patron, '\\'),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("nacionalidad")), patron, '\\'));

        return autorRepository.findAll(filtro, orden).stream()
                .map(autorMapper::aRespuesta)
                .toList();
    }

    @Transactional
    public AutorRespuesta actualizar(long id, AutorSolicitud solicitud) {
        var autor = buscarPorId(id);
        autor.actualizar(solicitud.nombre(), solicitud.apellido(), solicitud.nacionalidad());
        return autorMapper.aRespuesta(autor);
    }

    @Transactional
    public void eliminar(long id) {
        autorRepository.delete(buscarPorId(id));
    }

    private Autor buscarPorId(long id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Autor no encontrado"));
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
}
