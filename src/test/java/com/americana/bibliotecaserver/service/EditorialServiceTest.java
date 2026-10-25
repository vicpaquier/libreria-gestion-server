package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.EditorialSolicitud;
import com.americana.bibliotecaserver.entity.Editorial;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.EditorialMapper;
import com.americana.bibliotecaserver.repository.EditorialRepository;
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
class EditorialServiceTest {

    @Mock
    private EditorialRepository editorialRepository;

    private EditorialService editorialService;

    @BeforeEach
    void configurar() {
        editorialService = new EditorialService(editorialRepository, new EditorialMapper());
    }

    @Test
    void creaConValoresRecortadosYPaisVacioNulo() {
        when(editorialRepository.saveAndFlush(any(Editorial.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = editorialService.crear(new EditorialSolicitud("  Norma  ", "   "));

        assertThat(respuesta.nombre()).isEqualTo("Norma");
        assertThat(respuesta.pais()).isNull();
        verify(editorialRepository).existsByNombre("Norma");
    }

    @Test
    void listaEnOrdenAscendenteDeId() {
        when(editorialRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(new Editorial("Norma", "Colombia")));

        assertThat(editorialService.listar()).singleElement()
                .extracting("nombre").isEqualTo("Norma");

        var orden = ArgumentCaptor.forClass(Sort.class);
        verify(editorialRepository).findAll(orden.capture());
        assertThat(orden.getValue().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void rechazaNombreDuplicadoEnPrecheckDeCreacion() {
        when(editorialRepository.existsByNombre("Norma")).thenReturn(true);

        assertThatThrownBy(() -> editorialService.crear(
                new EditorialSolicitud(" Norma ", null)))
                .isInstanceOf(ConflictoUnicidadException.class)
                .hasMessage("El nombre de la editorial ya está registrado");
        verify(editorialRepository, never()).saveAndFlush(any());
    }

    @Test
    void permiteConservarElNombreAlActualizarYNormalizaPais() {
        var editorial = new Editorial("Norma", "Colombia");
        when(editorialRepository.findById(7L)).thenReturn(Optional.of(editorial));
        when(editorialRepository.saveAndFlush(editorial)).thenReturn(editorial);

        var respuesta = editorialService.actualizar(
                7L, new EditorialSolicitud(" Norma ", " Argentina "));

        assertThat(respuesta.nombre()).isEqualTo("Norma");
        assertThat(respuesta.pais()).isEqualTo("Argentina");
        verify(editorialRepository).existsByNombreAndIdNot("Norma", 7L);
    }

    @Test
    void rechazaNombreDeOtraEditorialAlActualizar() {
        when(editorialRepository.findById(7L))
                .thenReturn(Optional.of(new Editorial("Norma", null)));
        when(editorialRepository.existsByNombreAndIdNot("Planeta", 7L)).thenReturn(true);

        assertThatThrownBy(() -> editorialService.actualizar(
                7L, new EditorialSolicitud("Planeta", null)))
                .isInstanceOf(ConflictoUnicidadException.class);
        verify(editorialRepository, never()).saveAndFlush(any());
    }

    @Test
    void traduceCarreraDeBaseDeDatosEnCreacionYActualizacion() {
        var violacion = new DataIntegrityViolationException("uq_editoriales_nombre");
        when(editorialRepository.saveAndFlush(any(Editorial.class))).thenThrow(violacion);

        assertThatThrownBy(() -> editorialService.crear(new EditorialSolicitud("Norma", null)))
                .isInstanceOf(ConflictoUnicidadException.class);

        var editorial = new Editorial("Planeta", null);
        when(editorialRepository.findById(7L)).thenReturn(Optional.of(editorial));
        assertThatThrownBy(() -> editorialService.actualizar(
                7L, new EditorialSolicitud("Norma", null)))
                .isInstanceOf(ConflictoUnicidadException.class);
    }

    @Test
    void traduceBorradoConcurrenteDuranteActualizacionANoEncontrado() {
        var editorial = new Editorial("Norma", null);
        when(editorialRepository.findById(7L)).thenReturn(Optional.of(editorial));
        when(editorialRepository.saveAndFlush(editorial)).thenThrow(
                new ObjectOptimisticLockingFailureException(Editorial.class, 7L));

        assertThatThrownBy(() -> editorialService.actualizar(
                7L, new EditorialSolicitud("Planeta", null)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Editorial no encontrada");
    }

    @Test
    void traduceBorradoConcurrenteDuranteEliminacionANoEncontradoYFuerzaFlush() {
        var editorial = new Editorial("Norma", null);
        when(editorialRepository.findById(7L)).thenReturn(Optional.of(editorial));
        doThrow(new ObjectOptimisticLockingFailureException(Editorial.class, 7L))
                .when(editorialRepository).flush();

        assertThatThrownBy(() -> editorialService.eliminar(7L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Editorial no encontrada");
        verify(editorialRepository).delete(editorial);
        verify(editorialRepository).flush();
    }

    @Test
    void eliminaYRespondeNoEncontrado() {
        var editorial = new Editorial("Norma", null);
        when(editorialRepository.findById(7L)).thenReturn(Optional.of(editorial));
        when(editorialRepository.findById(99L)).thenReturn(Optional.empty());

        editorialService.eliminar(7L);

        verify(editorialRepository).delete(editorial);
        assertThatThrownBy(() -> editorialService.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Editorial no encontrada");
    }
}
