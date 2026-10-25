package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.ActualizarLibroSolicitud;
import com.americana.bibliotecaserver.dto.CrearLibroSolicitud;
import com.americana.bibliotecaserver.dto.LibroDetalleRespuesta;
import com.americana.bibliotecaserver.dto.LibroRespuesta;
import com.americana.bibliotecaserver.dto.PaginaLibrosRespuesta;
import com.americana.bibliotecaserver.entity.Autor;
import com.americana.bibliotecaserver.entity.Categoria;
import com.americana.bibliotecaserver.entity.Editorial;
import com.americana.bibliotecaserver.entity.EstadoDetallePrestamo;
import com.americana.bibliotecaserver.entity.Libro;
import com.americana.bibliotecaserver.entity.EstadoReserva;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.LibroMapper;
import com.americana.bibliotecaserver.mapper.PrestamoMapper;
import com.americana.bibliotecaserver.mapper.ReservaMapper;
import com.americana.bibliotecaserver.repository.AutorRepository;
import com.americana.bibliotecaserver.repository.CategoriaRepository;
import com.americana.bibliotecaserver.repository.EditorialRepository;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.repository.ReservaRepository;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LibroService {

    private static final String RESTRICCION_ISBN = "uq_libros_isbn";

    private final LibroRepository libroRepository;
    private final AutorRepository autorRepository;
    private final CategoriaRepository categoriaRepository;
    private final EditorialRepository editorialRepository;
    private final PrestamoRepository prestamoRepository;
    private final LibroMapper libroMapper;
    private final PrestamoMapper prestamoMapper;
    private final ReservaRepository reservaRepository;
    private final ReservaMapper reservaMapper;

    public LibroService(LibroRepository libroRepository, AutorRepository autorRepository,
            CategoriaRepository categoriaRepository, EditorialRepository editorialRepository,
            PrestamoRepository prestamoRepository, LibroMapper libroMapper,
            PrestamoMapper prestamoMapper, ReservaRepository reservaRepository,
            ReservaMapper reservaMapper) {
        this.libroRepository = libroRepository;
        this.autorRepository = autorRepository;
        this.categoriaRepository = categoriaRepository;
        this.editorialRepository = editorialRepository;
        this.prestamoRepository = prestamoRepository;
        this.libroMapper = libroMapper;
        this.prestamoMapper = prestamoMapper;
        this.reservaRepository = reservaRepository;
        this.reservaMapper = reservaMapper;
    }

    @Transactional
    public LibroRespuesta crear(CrearLibroSolicitud solicitud) {
        var isbn = normalizarIsbn(solicitud.isbn());
        validarIsbnDisponible(isbn, null);
        var editorial = buscarEditorial(solicitud.editorialId());
        var autores = buscarAutores(solicitud.autoresIds());
        var categorias = buscarCategorias(solicitud.categoriasIds());
        var libro = new Libro(solicitud.titulo().trim(), isbn, solicitud.anioPublicacion(),
                solicitud.numeroPaginas(), solicitud.cantidadTotal(), editorial, autores, categorias);
        return guardar(libro);
    }

    @Transactional(readOnly = true)
    public PaginaLibrosRespuesta listar(int page, int size, String buscar, Long autorId,
            Long categoriaId, Long editorialId, Integer anio, Boolean disponible) {
        var patron = crearPatronBusqueda(buscar);
        Specification<Libro> filtros = (root, query, cb) -> {
            query.distinct(true);
            var predicado = cb.conjunction();
            if (patron != null) {
                var autorBusqueda = root.join("autores", JoinType.LEFT);
                var categoriaBusqueda = root.join("categorias", JoinType.LEFT);
                var editorialBusqueda = root.join("editorial", JoinType.LEFT);
                predicado = cb.and(predicado, cb.or(
                        cb.like(cb.lower(root.get("titulo")), patron, '\\'),
                        cb.like(cb.lower(root.get("isbn")), patron, '\\'),
                        cb.like(cb.lower(cb.concat(cb.concat(autorBusqueda.get("nombre"), " "),
                                autorBusqueda.get("apellido"))), patron, '\\'),
                        cb.like(cb.lower(editorialBusqueda.get("nombre")), patron, '\\'),
                        cb.like(cb.lower(categoriaBusqueda.get("nombre")), patron, '\\')));
            }
            if (autorId != null) {
                var autorFiltro = root.join("autores", JoinType.INNER);
                predicado = cb.and(predicado, cb.equal(autorFiltro.get("id"), autorId));
            }
            if (categoriaId != null) {
                var categoriaFiltro = root.join("categorias", JoinType.INNER);
                predicado = cb.and(predicado, cb.equal(categoriaFiltro.get("id"), categoriaId));
            }
            if (editorialId != null) {
                predicado = cb.and(predicado,
                        cb.equal(root.get("editorial").get("id"), editorialId));
            }
            if (anio != null) predicado = cb.and(predicado, cb.equal(root.get("anioPublicacion"), anio));
            if (disponible != null) {
                predicado = cb.and(predicado, disponible
                        ? cb.greaterThan(root.get("cantidadDisponible"), 0)
                        : cb.equal(root.get("cantidadDisponible"), 0));
            }
            return predicado;
        };
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var pagina = libroRepository.findAll(filtros, pageable);
        var ids = pagina.getContent().stream().map(Libro::getId).toList();
        var contenido = ids.isEmpty() ? List.<LibroRespuesta>of()
                : libroRepository.buscarTodosConRelaciones(ids).stream()
                        .map(libroMapper::aRespuesta).toList();
        return new PaginaLibrosRespuesta(page, size, pagina.getTotalElements(),
                pagina.getTotalPages(), contenido);
    }

    @Transactional(readOnly = true)
    public LibroDetalleRespuesta consultar(long id) {
        var libro = buscarPorId(id);
        var prestamos = prestamoRepository.buscarPorLibroYEstadoDetalle(
                        id, EstadoDetallePrestamo.PRESTADO).stream()
                .map(prestamoMapper::aResumen)
                .toList();
        var reservas = reservaRepository.findByLibroIdAndEstadoOrderByIdAsc(
                        id, EstadoReserva.PENDIENTE).stream()
                .map(reservaMapper::aRespuesta)
                .toList();
        return libroMapper.aDetalle(libro, prestamos, reservas);
    }

    @Transactional
    public LibroRespuesta actualizar(long id, ActualizarLibroSolicitud solicitud) {
        var libro = buscarPorIdConBloqueo(id);
        var isbn = normalizarIsbn(solicitud.isbn());
        validarIsbnDisponible(isbn, id);
        libro.actualizar(solicitud.titulo().trim(), isbn, solicitud.anioPublicacion(),
                solicitud.numeroPaginas(), buscarEditorial(solicitud.editorialId()),
                buscarAutores(solicitud.autoresIds()), buscarCategorias(solicitud.categoriasIds()));
        return guardar(libro);
    }

    @Transactional
    public LibroRespuesta ajustarStock(long id, int nuevaCantidadTotal) {
        var libro = buscarPorIdConBloqueo(id);
        int unidadesNoDisponibles = libro.getCantidadTotal() - libro.getCantidadDisponible();
        if (nuevaCantidadTotal < unidadesNoDisponibles) {
            throw new ConflictoNegocioException("STOCK_INSUFICIENTE", "cantidadTotal",
                    "La cantidad total no puede ser menor que las unidades no disponibles");
        }
        libro.ajustarCantidadTotal(nuevaCantidadTotal);
        return libroMapper.aRespuesta(libroRepository.saveAndFlush(libro));
    }

    private LibroRespuesta guardar(Libro libro) {
        try {
            return libroMapper.aRespuesta(libroRepository.saveAndFlush(libro));
        } catch (DataIntegrityViolationException exception) {
            if (esRestriccion(exception, RESTRICCION_ISBN)) throw conflictoIsbn();
            throw exception;
        }
    }

    private void validarIsbnDisponible(String isbn, Long idActual) {
        if (isbn == null) return;
        boolean existe = idActual == null
                ? libroRepository.existsByIsbn(isbn)
                : libroRepository.existsByIsbnAndIdNot(isbn, idActual);
        if (existe) throw conflictoIsbn();
    }

    private Editorial buscarEditorial(Long id) {
        if (id == null) return null;
        return editorialRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Editorial no encontrada"));
    }

    private Set<Autor> buscarAutores(List<Long> ids) {
        var encontrados = autorRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Autor::getId)).toList();
        if (encontrados.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Uno o más autores no existen");
        }
        return new LinkedHashSet<>(encontrados);
    }

    private Set<Categoria> buscarCategorias(List<Long> ids) {
        var encontradas = categoriaRepository.findAllById(ids).stream()
                .sorted(Comparator.comparing(Categoria::getId)).toList();
        if (encontradas.size() != ids.size()) {
            throw new RecursoNoEncontradoException("Una o más categorías no existen");
        }
        return new LinkedHashSet<>(encontradas);
    }

    private Libro buscarPorId(long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado"));
    }

    private Libro buscarPorIdConBloqueo(long id) {
        return libroRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Libro no encontrado"));
    }

    private String normalizarIsbn(String isbn) {
        return isbn == null || isbn.trim().isEmpty() ? null : isbn.trim();
    }

    private String crearPatronBusqueda(String buscar) {
        if (buscar == null || buscar.trim().isEmpty()) return null;
        return "%" + buscar.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private boolean esRestriccion(DataIntegrityViolationException exception, String restriccion) {
        if (exception.getCause() instanceof ConstraintViolationException violacion
                && restriccion.equals(violacion.getConstraintName())) return true;
        var detalle = exception.getMostSpecificCause().getMessage();
        return detalle != null && detalle.contains(restriccion);
    }

    private ConflictoUnicidadException conflictoIsbn() {
        return new ConflictoUnicidadException(
                "ISBN_DUPLICADO", "isbn", "El ISBN ya está registrado");
    }
}
