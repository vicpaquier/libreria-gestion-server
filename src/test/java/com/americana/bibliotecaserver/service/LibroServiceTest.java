package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CrearLibroSolicitud;
import com.americana.bibliotecaserver.dto.ActualizarLibroSolicitud;
import com.americana.bibliotecaserver.entity.Autor;
import com.americana.bibliotecaserver.entity.Categoria;
import com.americana.bibliotecaserver.entity.Libro;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.LibroMapper;
import com.americana.bibliotecaserver.mapper.PrestamoMapper;
import com.americana.bibliotecaserver.repository.AutorRepository;
import com.americana.bibliotecaserver.repository.CategoriaRepository;
import com.americana.bibliotecaserver.repository.EditorialRepository;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.repository.ReservaRepository;
import com.americana.bibliotecaserver.mapper.ReservaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock LibroRepository libroRepository;
    @Mock AutorRepository autorRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock EditorialRepository editorialRepository;
    @Mock PrestamoRepository prestamoRepository;
    @Mock LibroMapper libroMapper;
    @Mock PrestamoMapper prestamoMapper;
    @Mock ReservaRepository reservaRepository;
    @Mock ReservaMapper reservaMapper;

    private LibroService libroService;

    @BeforeEach
    void configurar() {
        libroService = new LibroService(libroRepository, autorRepository, categoriaRepository,
                editorialRepository, prestamoRepository, libroMapper, prestamoMapper,
                reservaRepository, reservaMapper);
    }

    @Test
    void creaDisponibleIgualAlTotalYConvierteIsbnVacioEnNulo() {
        when(autorRepository.findAllById(List.of(1L))).thenReturn(List.of(new Autor("Ana", "Paz", null)));
        when(categoriaRepository.findAllById(List.of(2L))).thenReturn(List.of(new Categoria("Novela", null)));
        when(libroRepository.saveAndFlush(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        libroService.crear(new CrearLibroSolicitud(
                " Libro ", "   ", 2020, 100, 5, null, List.of(1L), List.of(2L)));

        var captor = ArgumentCaptor.forClass(Libro.class);
        verify(libroRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTitulo()).isEqualTo("Libro");
        assertThat(captor.getValue().getIsbn()).isNull();
        assertThat(captor.getValue().getCantidadDisponible()).isEqualTo(5);
    }

    @Test
    void rechazaReferenciasInexistentesAntesDeGuardar() {
        when(autorRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(new Autor("Ana", "Paz", null)));

        assertThatThrownBy(() -> libroService.crear(new CrearLibroSolicitud(
                "Libro", null, null, null, 1, null, List.of(1L, 2L), List.of(3L))))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Uno o más autores no existen");
        verify(libroRepository, never()).saveAndFlush(any());
    }

    @Test
    void traduceCarreraDeIsbnEnConflicto() {
        when(autorRepository.findAllById(List.of(1L))).thenReturn(List.of(new Autor("Ana", "Paz", null)));
        when(categoriaRepository.findAllById(List.of(2L))).thenReturn(List.of(new Categoria("Novela", null)));
        when(libroRepository.saveAndFlush(any())).thenThrow(
                new DataIntegrityViolationException("violación uq_libros_isbn"));

        assertThatThrownBy(() -> libroService.crear(new CrearLibroSolicitud(
                "Libro", "ISBN", null, null, 1, null, List.of(1L), List.of(2L))))
                .isInstanceOf(ConflictoUnicidadException.class)
                .hasMessage("El ISBN ya está registrado");
    }

    @Test
    void stockUsaBloqueoYRechazaTotalMenorQueNoDisponibles() {
        var libro = org.mockito.Mockito.mock(Libro.class);
        when(libro.getCantidadTotal()).thenReturn(5);
        when(libro.getCantidadDisponible()).thenReturn(2);
        when(libroRepository.buscarPorIdConBloqueo(8L)).thenReturn(Optional.of(libro));

        assertThatThrownBy(() -> libroService.ajustarStock(8L, 2))
                .isInstanceOf(ConflictoNegocioException.class)
                .hasMessageContaining("unidades no disponibles");
        verify(libro, never()).ajustarCantidadTotal(any(Integer.class));
    }

    @Test
    void actualizarBloqueaElLibroYPreservaElStockLeidoBajoLock() {
        var libro = new Libro("Anterior", null, null, null, 5, null,
                List.of(), List.of());
        when(libroRepository.buscarPorIdConBloqueo(8L)).thenReturn(Optional.of(libro));
        when(autorRepository.findAllById(List.of(1L)))
                .thenReturn(List.of(new Autor("Ana", "Paz", null)));
        when(categoriaRepository.findAllById(List.of(2L)))
                .thenReturn(List.of(new Categoria("Novela", null)));
        when(libroRepository.saveAndFlush(libro)).thenReturn(libro);

        libroService.actualizar(8L, new ActualizarLibroSolicitud(
                "Nuevo", null, 2026, 200, null, List.of(1L), List.of(2L)));

        verify(libroRepository).buscarPorIdConBloqueo(8L);
        verify(libroRepository, never()).findById(8L);
        assertThat(libro.getTitulo()).isEqualTo("Nuevo");
        assertThat(libro.getCantidadTotal()).isEqualTo(5);
        assertThat(libro.getCantidadDisponible()).isEqualTo(5);
    }
}
