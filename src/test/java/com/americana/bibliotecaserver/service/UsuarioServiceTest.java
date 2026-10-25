package com.americana.bibliotecaserver.service;

import com.americana.bibliotecaserver.dto.ActualizarUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CambiarEstadoUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CrearUsuarioSolicitud;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.entity.Usuario;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.mapper.UsuarioMapper;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    @BeforeEach
    void configurar() {
        usuarioService = new UsuarioService(usuarioRepository, new UsuarioMapper());
    }

    @Test
    void creaUsuarioActivoConFechaAutomatica() {
        var solicitud = solicitud("123", "ana@example.com");
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = usuarioService.crear(solicitud);

        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(respuesta.fechaRegistro()).isNotNull();
        assertThat(respuesta.cedula()).isEqualTo("123");
        verify(usuarioRepository).existsByCedula("123");
        verify(usuarioRepository).existsByEmail("ana@example.com");
    }

    @Test
    void rechazaCedulaDuplicadaAntesDeGuardar() {
        when(usuarioRepository.existsByCedula("123")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(solicitud("123", null)))
                .isInstanceOf(ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("cedula");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaEmailDuplicadoAntesDeGuardar() {
        when(usuarioRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(solicitud("123", "ana@example.com")))
                .isInstanceOf(ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("email");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void traduceCarreraDeCedulaProtegidaPorConstraint() {
        var causa = new RuntimeException("violates unique constraint \"uq_usuarios_cedula\"");
        when(usuarioRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicado", causa));

        assertThatThrownBy(() -> usuarioService.crear(solicitud("123", null)))
                .isInstanceOf(ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("cedula");
    }

    @Test
    void traduceCarreraDeEmailProtegidaPorConstraint() {
        var causa = new RuntimeException("violates unique constraint \"uq_usuarios_email\"");
        when(usuarioRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicado", causa));

        assertThatThrownBy(() -> usuarioService.crear(solicitud("123", "ana@example.com")))
                .isInstanceOf(ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("email");
    }

    @Test
    void priorizaNombreEstructuradoDeConstraint() {
        var causa = new ConstraintViolationException(
                "violates uq_usuarios_cedula",
                new SQLException("detalle sin nombre de constraint"),
                "uq_usuarios_email");
        when(usuarioRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicado", causa));

        assertThatThrownBy(() -> usuarioService.crear(solicitud("123", "ana@example.com")))
                .isInstanceOf(ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("email");
    }

    @Test
    void normalizaEmailVacioANullSinConsultarUnicidad() {
        when(usuarioRepository.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = usuarioService.crear(solicitud("123", ""));

        assertThat(respuesta.email()).isNull();
        verify(usuarioRepository, never()).existsByEmail(any());
    }

    @Test
    void actualizaSoloDatosEditablesConBloqueo() {
        var usuario = new Usuario("Ana", "Pérez", "123", "anterior@example.com", "111", "Calle 1");
        var fechaRegistro = usuario.getFechaRegistro();
        when(usuarioRepository.buscarPorIdConBloqueo(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        var respuesta = usuarioService.actualizar(7L, new ActualizarUsuarioSolicitud(
                "Inés", "Rojas", "ines@example.com", "222", "Calle 2"));

        assertThat(respuesta.nombre()).isEqualTo("Inés");
        assertThat(respuesta.apellido()).isEqualTo("Rojas");
        assertThat(respuesta.email()).isEqualTo("ines@example.com");
        assertThat(respuesta.telefono()).isEqualTo("222");
        assertThat(respuesta.direccion()).isEqualTo("Calle 2");
        assertThat(respuesta.cedula()).isEqualTo("123");
        assertThat(respuesta.fechaRegistro()).isEqualTo(fechaRegistro);
        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(usuarioRepository).existsByEmailAndIdNot("ines@example.com", 7L);
    }

    @Test
    void normalizaEmailVacioAlActualizarSinCambiarOtrosOpcionales() {
        var usuario = new Usuario("Ana", "Pérez", "123", "anterior@example.com", "111", "Calle 1");
        when(usuarioRepository.buscarPorIdConBloqueo(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        var respuesta = usuarioService.actualizar(
                7L, new ActualizarUsuarioSolicitud("Ana", "Pérez", "", "", " "));

        assertThat(respuesta.email()).isNull();
        assertThat(respuesta.telefono()).isEmpty();
        assertThat(respuesta.direccion()).isEqualTo(" ");
        verify(usuarioRepository, never()).existsByEmailAndIdNot(any(), any());
    }

    @Test
    void rechazaEmailDeOtroUsuarioAntesDeActualizar() {
        var usuario = new Usuario("Ana", "Pérez", "123", null, null, null);
        when(usuarioRepository.buscarPorIdConBloqueo(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.existsByEmailAndIdNot("usado@example.com", 7L)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizar(7L, new ActualizarUsuarioSolicitud(
                "Ana", "Pérez", "usado@example.com", null, null)))
                .isInstanceOf(ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("email");
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void traduceCarreraDeEmailAlActualizar() {
        var usuario = new Usuario("Ana", "Pérez", "123", null, null, null);
        when(usuarioRepository.buscarPorIdConBloqueo(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenThrow(new DataIntegrityViolationException(
                "duplicado", new RuntimeException("violates unique constraint \"uq_usuarios_email\"")));

        assertThatThrownBy(() -> usuarioService.actualizar(7L, new ActualizarUsuarioSolicitud(
                "Ana", "Pérez", "carrera@example.com", null, null)))
                .isInstanceOf(ConflictoUnicidadException.class)
                .extracting("campo")
                .isEqualTo("email");
    }

    @Test
    void cambiaEstadoConBloqueoSinModificarDatosPersonales() {
        var usuario = new Usuario("Ana", "Pérez", "123", "ana@example.com", "111", "Calle 1");
        when(usuarioRepository.buscarPorIdConBloqueo(7L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.saveAndFlush(usuario)).thenReturn(usuario);

        var respuesta = usuarioService.cambiarEstado(
                7L, new CambiarEstadoUsuarioSolicitud(EstadoUsuario.INACTIVO));

        assertThat(respuesta.estado()).isEqualTo(EstadoUsuario.INACTIVO);
        assertThat(respuesta.nombre()).isEqualTo("Ana");
        assertThat(respuesta.cedula()).isEqualTo("123");
    }

    @Test
    void rechazaUsuarioInexistenteEnAmbasMutaciones() {
        when(usuarioRepository.buscarPorIdConBloqueo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizar(99L,
                new ActualizarUsuarioSolicitud("Ana", "Pérez", null, null, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> usuarioService.cambiarEstado(99L,
                new CambiarEstadoUsuarioSolicitud(EstadoUsuario.ACTIVO)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void listaUnaPaginaMapeadaYOrdenadaPorIdAscendente() {
        var usuario = new Usuario("Ana", "Pérez", "123", null, null, null);
        when(usuarioRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<Usuario>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(usuario), Pageable.ofSize(2).withPage(1), 5));

        var respuesta = usuarioService.listar(1, 2, EstadoUsuario.ACTIVO, " ana ");

        assertThat(respuesta.page()).isEqualTo(1);
        assertThat(respuesta.size()).isEqualTo(2);
        assertThat(respuesta.totalElements()).isEqualTo(5);
        assertThat(respuesta.totalPages()).isEqualTo(3);
        assertThat(respuesta.content()).singleElement()
                .extracting("cedula")
                .isEqualTo("123");
        var paginacion = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioRepository).findAll(
                org.mockito.ArgumentMatchers.<Specification<Usuario>>any(), paginacion.capture());
        assertThat(paginacion.getValue().getSort().getOrderFor("id").isAscending()).isTrue();
    }

    private CrearUsuarioSolicitud solicitud(String cedula, String email) {
        return new CrearUsuarioSolicitud("Ana", "Pérez", cedula, email, null, null);
    }
}
