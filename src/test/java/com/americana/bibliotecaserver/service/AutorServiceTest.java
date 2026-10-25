package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.AutorSolicitud;
import com.americana.bibliotecaserver.entity.Autor;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.AutorMapper;
import com.americana.bibliotecaserver.repository.AutorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorServiceTest {

    @Mock
    private AutorRepository autorRepository;

    private AutorService autorService;

    @BeforeEach
    void configurar() {
        autorService = new AutorService(autorRepository, new AutorMapper());
    }

    @Test
    void creaAutorConNacionalidadOpcional() {
        when(autorRepository.save(any(Autor.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = autorService.crear(new AutorSolicitud("Ana", "Rojas", null));

        assertThat(respuesta.nombre()).isEqualTo("Ana");
        assertThat(respuesta.apellido()).isEqualTo("Rojas");
        assertThat(respuesta.nacionalidad()).isNull();
    }

    @Test
    void listaEnOrdenAscendenteDeId() {
        when(autorRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Autor>>any(), any(Sort.class)))
                .thenReturn(List.of(new Autor("Ana", "Rojas", "Colombiana")));

        var respuesta = autorService.listar(" ana ");

        assertThat(respuesta).singleElement().extracting("nombre").isEqualTo("Ana");
        var orden = ArgumentCaptor.forClass(Sort.class);
        verify(autorRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<Autor>>any(), orden.capture());
        assertThat(orden.getValue().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void listaSinSpecificationCuandoLaBusquedaEstaVacia() {
        when(autorRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertThat(autorService.listar("   ")).isEmpty();

        var orden = ArgumentCaptor.forClass(Sort.class);
        verify(autorRepository).findAll(orden.capture());
        assertThat(orden.getValue().getOrderFor("id").isAscending()).isTrue();
    }

    @Test
    void actualizaTodosLosCampos() {
        var autor = new Autor("Ana", "Rojas", null);
        when(autorRepository.findById(7L)).thenReturn(Optional.of(autor));

        var respuesta = autorService.actualizar(
                7L, new AutorSolicitud("Inés", "Paz", "Argentina"));

        assertThat(respuesta.nombre()).isEqualTo("Inés");
        assertThat(respuesta.apellido()).isEqualTo("Paz");
        assertThat(respuesta.nacionalidad()).isEqualTo("Argentina");
    }

    @Test
    void eliminaAutorExistente() {
        var autor = new Autor("Ana", "Rojas", null);
        when(autorRepository.findById(7L)).thenReturn(Optional.of(autor));

        autorService.eliminar(7L);

        verify(autorRepository).delete(autor);
    }

    @Test
    void respondeNoEncontradoAlActualizarOEliminar() {
        when(autorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autorService.actualizar(
                99L, new AutorSolicitud("Ana", "Rojas", null)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Autor no encontrado");
        assertThatThrownBy(() -> autorService.eliminar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("Autor no encontrado");
        verify(autorRepository, never()).delete(any(Autor.class));
    }
}
