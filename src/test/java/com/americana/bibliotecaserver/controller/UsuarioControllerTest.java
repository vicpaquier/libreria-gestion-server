package com.americana.bibliotecaserver.controller;

import com.americana.bibliotecaserver.dto.ActualizarUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CambiarEstadoUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.CrearUsuarioSolicitud;
import com.americana.bibliotecaserver.dto.PaginaUsuariosRespuesta;
import com.americana.bibliotecaserver.dto.UsuarioRespuesta;
import com.americana.bibliotecaserver.entity.EstadoUsuario;
import com.americana.bibliotecaserver.exception.ApiExceptionHandler;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.exception.RecursoNoEncontradoException;
import com.americana.bibliotecaserver.service.UsuarioService;
import com.americana.bibliotecaserver.service.HistorialUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UsuarioControllerTest {

    private UsuarioService usuarioService;
    private MockMvc mockMvc;
    private HistorialUsuarioService historialUsuarioService;

    @BeforeEach
    void configurar() {
        usuarioService = mock(UsuarioService.class);
        historialUsuarioService = mock(HistorialUsuarioService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UsuarioController(usuarioService, historialUsuarioService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void responde201ConUsuarioCreado() throws Exception {
        var fecha = OffsetDateTime.parse("2026-09-06T10:15:30Z");
        when(usuarioService.crear(any())).thenReturn(new UsuarioRespuesta(
                1L, "Ana", "Pérez", "123", null, null, null, fecha, EstadoUsuario.ACTIVO));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","cedula":"123","email":null}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaRegistro").value("2026-09-06T10:15:30Z"));
        verify(usuarioService).crear(any(CrearUsuarioSolicitud.class));
    }

    @Test
    void responde400ConCampoParaNombreVacio() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","apellido":"Pérez","cedula":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("nombre"))
                .andExpect(jsonPath("$.path").value("/api/usuarios"));
    }

    @Test
    void normalizaEmailVacioANullAlCrear() throws Exception {
        when(usuarioService.crear(any())).thenReturn(new UsuarioRespuesta(
                1L, "Ana", "Pérez", "123", null, null, null,
                OffsetDateTime.parse("2026-09-06T10:15:30Z"), EstadoUsuario.ACTIVO));
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","cedula":"123","email":""}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(nullValue()));
        var solicitud = ArgumentCaptor.forClass(CrearUsuarioSolicitud.class);
        verify(usuarioService).crear(solicitud.capture());
        org.assertj.core.api.Assertions.assertThat(solicitud.getValue().email()).isNull();
    }

    @Test
    void responde400ConCampoParaFormatoDeEmailInvalido() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","cedula":"123","email":"invalido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("email"));
    }

    @Test
    void responde409ConCampoDuplicado() throws Exception {
        when(usuarioService.crear(any())).thenThrow(new ConflictoUnicidadException(
                "CEDULA_DUPLICADA", "cedula", "La cédula ya está registrada"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","cedula":"123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CEDULA_DUPLICADA"))
                .andExpect(jsonPath("$.campo").value("cedula"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void respondeErrorInternoSinExponerDetallesTecnicos() throws Exception {
        when(usuarioService.crear(any())).thenThrow(new IllegalStateException("detalle sensible"));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Pérez","cedula":"123"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("No fue posible completar la solicitud"))
                .andExpect(jsonPath("$.path").value("/api/usuarios"));
    }

    @Test
    void listaConValoresPorDefectoSinExponerPageDeSpring() throws Exception {
        var fecha = OffsetDateTime.parse("2026-09-06T10:15:30Z");
        var usuario = new UsuarioRespuesta(
                1L, "Ana", "Pérez", "123", null, null, null, fecha, EstadoUsuario.ACTIVO);
        when(usuarioService.listar(0, 20, null, null)).thenReturn(
                new PaginaUsuariosRespuesta(0, 20, 1, 1, List.of(usuario)));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].cedula").value("123"));
        verify(usuarioService).listar(0, 20, null, null);
    }

    @Test
    void pasaFiltrosYPaginacionAlServicio() throws Exception {
        when(usuarioService.listar(2, 10, EstadoUsuario.INACTIVO, " Ana ")).thenReturn(
                new PaginaUsuariosRespuesta(2, 10, 0, 0, List.of()));

        mockMvc.perform(get("/api/usuarios")
                        .param("page", "2")
                        .param("size", "10")
                        .param("estado", "INACTIVO")
                        .param("buscar", " Ana "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
        verify(usuarioService).listar(2, 10, EstadoUsuario.INACTIVO, " Ana ");
    }

    @Test
    void responde400ConCampoParaEstadoInvalido() throws Exception {
        mockMvc.perform(get("/api/usuarios").param("estado", "DESCONOCIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("estado"));

        mockMvc.perform(get("/api/usuarios").param("estado", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("estado"));
    }

    @Test
    void responde400ConCampoParaTipoDePaginaInvalido() throws Exception {
        mockMvc.perform(get("/api/usuarios").param("page", "uno"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("page"));
    }

    @Test
    void responde400ConCampoParaRangosInvalidos() throws Exception {
        mockMvc.perform(get("/api/usuarios").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("page"));

        mockMvc.perform(get("/api/usuarios").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.campo").value("size"));
    }

    @Test
    void responde200AlActualizarDatosEditables() throws Exception {
        var fecha = OffsetDateTime.parse("2026-09-06T10:15:30Z");
        when(usuarioService.actualizar(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(
                new UsuarioRespuesta(7L, "Inés", "Rojas", "123", null, "", " ", fecha,
                        EstadoUsuario.ACTIVO));

        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Inés","apellido":"Rojas","email":"","telefono":"","direccion":" "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cedula").value("123"))
                .andExpect(jsonPath("$.email").value(nullValue()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
        var solicitud = ArgumentCaptor.forClass(ActualizarUsuarioSolicitud.class);
        verify(usuarioService).actualizar(org.mockito.ArgumentMatchers.eq(7L), solicitud.capture());
        org.assertj.core.api.Assertions.assertThat(solicitud.getValue().email()).isNull();
    }

    @Test
    void validaCuerpoEIdAlActualizar() throws Exception {
        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","apellido":"Rojas"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("nombre"));

        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Rojas","email":"invalido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("email"));

        mockMvc.perform(put("/api/usuarios/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Rojas"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("id"));
    }

    @Test
    void rechazaCamposNoEditablesEnElPut() throws Exception {
        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Rojas","cedula":"nueva"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void responde404Y409AlActualizar() throws Exception {
        when(usuarioService.actualizar(org.mockito.ArgumentMatchers.eq(7L), any()))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));
        when(usuarioService.actualizar(org.mockito.ArgumentMatchers.eq(8L), any()))
                .thenThrow(new ConflictoUnicidadException(
                        "EMAIL_DUPLICADO", "email", "El email ya está registrado"));

        mockMvc.perform(put("/api/usuarios/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Rojas"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NO_ENCONTRADO"));

        mockMvc.perform(put("/api/usuarios/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Ana","apellido":"Rojas","email":"usado@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EMAIL_DUPLICADO"))
                .andExpect(jsonPath("$.campo").value("email"));
    }

    @Test
    void responde200AlCambiarEstado() throws Exception {
        var fecha = OffsetDateTime.parse("2026-09-06T10:15:30Z");
        when(usuarioService.cambiarEstado(org.mockito.ArgumentMatchers.eq(7L), any())).thenReturn(
                new UsuarioRespuesta(7L, "Ana", "Pérez", "123", null, null, null, fecha,
                        EstadoUsuario.INACTIVO));

        mockMvc.perform(patch("/api/usuarios/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"INACTIVO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
        verify(usuarioService).cambiarEstado(org.mockito.ArgumentMatchers.eq(7L),
                any(CambiarEstadoUsuarioSolicitud.class));
    }

    @Test
    void validaEstadoEIdAlCambiarEstado() throws Exception {
        mockMvc.perform(patch("/api/usuarios/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"SUSPENDIDO"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/usuarios/7/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("estado"));

        mockMvc.perform(patch("/api/usuarios/-1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"ACTIVO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("id"));
    }

    @Test
    void responde404AlCambiarEstadoDeUsuarioInexistente() throws Exception {
        when(usuarioService.cambiarEstado(org.mockito.ArgumentMatchers.eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

        mockMvc.perform(patch("/api/usuarios/99/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"INACTIVO"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/usuarios/99/estado"));
    }

    @Test
    void consultaDetalleEHistorialConColeccionesDelContrato() throws Exception {
        var fecha = OffsetDateTime.parse("2026-09-06T10:15:30Z");
        when(historialUsuarioService.consultarDetalle(7L)).thenReturn(
                new com.americana.bibliotecaserver.dto.UsuarioDetalleRespuesta(
                        7L, "Ana", "Pérez", "123", null, null, null, fecha,
                        EstadoUsuario.ACTIVO, List.of(), List.of(), List.of()));
        when(historialUsuarioService.consultarHistorial(7L)).thenReturn(
                new com.americana.bibliotecaserver.dto.HistorialUsuarioRespuesta(
                        List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/usuarios/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.prestamos").isArray())
                .andExpect(jsonPath("$.reservas").isArray())
                .andExpect(jsonPath("$.multas").isArray());
        mockMvc.perform(get("/api/usuarios/7/historial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prestamos").isArray())
                .andExpect(jsonPath("$.reservas").isArray())
                .andExpect(jsonPath("$.multas").isArray());
    }
}
