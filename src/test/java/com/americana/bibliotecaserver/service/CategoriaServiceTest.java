package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.CategoriaSolicitud;
import com.americana.bibliotecaserver.entity.Categoria;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.CategoriaMapper;
import com.americana.bibliotecaserver.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private CategoriaService categoriaService;

    @BeforeEach
    void configurar() {
        categoriaService = new CategoriaService(categoriaRepository, new CategoriaMapper());
    }

    @Test
    void creaConValoresRecortadosYDescripcionVaciaNula() {
        when(categoriaRepository.saveAndFlush(any(Categoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = categoriaService.crear(new CategoriaSolicitud("  Novela  ", "   "));

        assertThat(respuesta.nombre()).isEqualTo("Novela");
        assertThat(respuesta.descripcion()).isNull();
        verify(categoriaRepository).existsByNombre("Novela");
    }

    @Test
    void listaEnOrdenAscendenteDeId() {
        when(categoriaRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(new Categoria("Novela", null)));

        assertThat(categoriaService.listar()).singleElement()
                .extracting("nombre").isEqualTo("Novela");

        var orden = ArgumentCaptor.forClass(Sort.class);
        verify(categoriaRepository).findAll(orden.capture());
        assertThat(orden.getValue().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void rechazaDuplicadoEnPrecheckYTraduceCarreraDeBaseDeDatos() {
        when(categoriaRepository.existsByNombre("Novela")).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.crear(
                new CategoriaSolicitud(" Novela ", null)))
                .isInstanceOf(ConflictoUnicidadException.class)
                .hasMessage("El nombre de la categoría ya está registrado");
        verify(categoriaRepository, never()).saveAndFlush(any());

        when(categoriaRepository.existsByNombre("Ensayo")).thenReturn(false);
        when(categoriaRepository.saveAndFlush(any(Categoria.class)))
                .thenThrow(new DataIntegrityViolationException("uq_categorias_nombre"));

        assertThatThrownBy(() -> categoriaService.crear(new CategoriaSolicitud("Ensayo", null)))
                .isInstanceOf(ConflictoUnicidadException.class);
    }

    @Test
    void actualizaPermitiendoSuNombreYNormalizaDescripcion() {
        var categoria = new Categoria("Novela", null);
        when(categoriaRepository.findById(7L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.saveAndFlush(categoria)).thenReturn(categoria);

        var respuesta = categoriaService.actualizar(
                7L, new CategoriaSolicitud(" Novela ", " Narrativa "));

        assertThat(respuesta.nombre()).isEqualTo("Novela");
        assertThat(respuesta.descripcion()).isEqualTo("Narrativa");
        verify(categoriaRepository).existsByNombreAndIdNot("Novela", 7L);
    }

    @Test
    void rechazaNombreAjenoYTraduceCarreraDeUnicidadAlActualizar() {
        var categoria = new Categoria("Novela", null);
        when(categoriaRepository.findById(7L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNombreAndIdNot("Ensayo", 7L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.actualizar(
                7L, new CategoriaSolicitud("Ensayo", null)))
                .isInstanceOf(ConflictoUnicidadException.class);

        when(categoriaRepository.existsByNombreAndIdNot("Ensayo", 7L)).thenReturn(false);
        when(categoriaRepository.saveAndFlush(categoria))
                .thenThrow(new DataIntegrityViolationException("uq_categorias_nombre"));

        assertThatThrownBy(() -> categoriaService.actualizar(
                7L, new CategoriaSolicitud("Ensayo", null)))
                .isInstanceOf(ConflictoUnicidadException.class);
    }

    @Test
    void traduceBorradoConcurrenteDuranteActualizacionYEliminacionANoEncontrado() {
        var categoria = new Categoria("Novela", null);
        when(categoriaRepository.findById(7L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.saveAndFlush(categoria)).thenThrow(
                new ObjectOptimisticLockingFailureException(Categoria.class, 7L));

        assertThatThrownBy(() -> categoriaService.actualizar(
                7L, new CategoriaSolicitud("Ensayo", null)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Categoría no encontrada");

        doThrow(new ObjectOptimisticLockingFailureException(Categoria.class, 7L))
                .when(categoriaRepository).flush();
        assertThatThrownBy(() -> categoriaService.eliminar(7L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Categoría no encontrada");
        verify(categoriaRepository).delete(categoria);
    }

    @Test
    void eliminaYRespondeNoEncontrado() {
        var categoria = new Categoria("Novela", null);
        when(categoriaRepository.findById(7L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        categoriaService.eliminar(7L);

        verify(categoriaRepository).delete(categoria);
        verify(categoriaRepository).flush();
        assertThatThrownBy(() -> categoriaService.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Categoría no encontrada");
    }
}
