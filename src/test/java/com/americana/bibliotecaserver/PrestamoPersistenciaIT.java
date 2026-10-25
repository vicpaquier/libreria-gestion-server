package com.americana.bibliotecaserver;

import com.americana.bibliotecaserver.dto.CrearPrestamoSolicitud;
import com.americana.bibliotecaserver.entity.EstadoDetallePrestamo;
import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.repository.PrestamoRepository;
import com.americana.bibliotecaserver.service.PrestamoService;
import com.americana.bibliotecaserver.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers(disabledWithoutDocker = true)
class PrestamoPersistenciaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PrestamoService prestamoService;
    @Autowired PrestamoRepository prestamoRepository;
    @Autowired ReservaService reservaService;

    @BeforeEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM detalle_prestamo");
        jdbcTemplate.update("DELETE FROM prestamos");
        jdbcTemplate.update("DELETE FROM libros");
        jdbcTemplate.update("DELETE FROM usuarios");
    }

    @Test
    void aplicaV6ConEstadosFksEIndices() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '6'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint
                WHERE conname IN ('ck_prestamos_estado', 'fk_prestamos_usuario',
                    'ck_detalle_prestamo_estado', 'fk_detalle_prestamo_prestamo',
                    'fk_detalle_prestamo_libro')
                """, String.class)).hasSize(5);
        assertThat(jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE indexname IN ('idx_prestamos_usuario', 'idx_prestamos_estado',
                    'idx_detalle_prestamo_prestamo', 'idx_detalle_prestamo_libro')
                """, String.class)).hasSize(4);
    }

    @Test
    void aplicaV7ConRestriccionesEIndicesDeReservasYMultas() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '7'", Boolean.class))
                .isTrue();
        assertThat(jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint
                WHERE conname IN ('ck_reservas_estado', 'fk_reservas_usuario',
                    'fk_reservas_libro', 'ck_multas_monto', 'ck_multas_estado',
                    'fk_multas_prestamo')
                """, String.class)).hasSize(6);
        assertThat(jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE indexname IN ('uq_reservas_pendiente_usuario_libro',
                    'idx_reservas_usuario', 'idx_reservas_libro', 'idx_reservas_estado',
                    'idx_multas_prestamo', 'idx_multas_estado', 'idx_multas_fecha')
                """, String.class)).hasSize(7);
    }

    @Test
    void reviertePrestamoCompletoCuandoUnoDeVariosLibrosNoEstaDisponible() {
        var usuario = usuario("100");
        var disponible = libro("Disponible", 1);
        var agotado = libro("Agotado", 0);

        assertThatThrownBy(() -> crear(usuario, List.of(disponible, agotado)))
                .isInstanceOf(ConflictoNegocioException.class);

        assertThat(prestamoRepository.count()).isZero();
        assertThat(stock(disponible)).isEqualTo(1);
        assertThat(stock(agotado)).isZero();
    }

    @Test
    void dosUsuariosCompitenPorLaUltimaUnidadYSoloUnPrestamoSeConfirma() throws Exception {
        var usuario1 = usuario("201");
        var usuario2 = usuario("202");
        var libro = libro("Última unidad", 1);

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primero = ejecutor.submit(() -> intentarCrear(usuario1, libro));
            var segundo = ejecutor.submit(() -> intentarCrear(usuario2, libro));
            assertThat(List.of(primero.get(), segundo.get()))
                    .containsExactlyInAnyOrder("CREADO", ConflictoNegocioException.class.getSimpleName());
        }
        assertThat(prestamoRepository.count()).isEqualTo(1);
        assertThat(stock(libro)).isZero();
    }

    @Test
    void devolverUnaSolaVezReponeStockYFinalizaElPrestamo() {
        var libro = libro("Devolución", 1);
        var creado = crear(usuario("301"), List.of(libro));
        var detalle = creado.detalles().getFirst().id();

        var devuelto = prestamoService.devolver(creado.id(), detalle);

        assertThat(devuelto.estado()).isEqualTo(EstadoPrestamo.DEVUELTO);
        assertThat(devuelto.detalles().getFirst().estado()).isEqualTo(EstadoDetallePrestamo.DEVUELTO);
        assertThat(devuelto.detalles().getFirst().fechaDevolucion()).isNotNull();
        assertThat(stock(libro)).isEqualTo(1);
        assertThatThrownBy(() -> prestamoService.devolver(creado.id(), detalle))
                .isInstanceOf(ConflictoNegocioException.class);
        assertThat(stock(libro)).isEqualTo(1);
    }

    @Test
    void perdidoFinalizaSinReponerStockYListadoCargaDetallesEnOrden() {
        var libro1 = libro("Uno", 1);
        var libro2 = libro("Dos", 1);
        var creado = crear(usuario("401"), List.of(libro2, libro1));
        var detalle1 = creado.detalles().getFirst();
        prestamoService.devolver(creado.id(), detalle1.id());
        var finalizado = prestamoService.marcarPerdido(
                creado.id(), creado.detalles().get(1).id());

        assertThat(finalizado.estado()).isEqualTo(EstadoPrestamo.DEVUELTO);
        assertThat(finalizado.detalles()).extracting("estado")
                .containsExactly(EstadoDetallePrestamo.DEVUELTO, EstadoDetallePrestamo.PERDIDO);
        assertThat(stock(finalizado.detalles().get(1).libro().id())).isZero();

        var pagina = prestamoService.listar(0, 20, EstadoPrestamo.DEVUELTO);
        assertThat(pagina.content()).extracting("id").containsExactly(creado.id());
        assertThat(pagina.content().getFirst().detalles()).extracting("id").isSorted();
    }

    @Test
    void detectaAtrasadosUnaSolaVezYCalculaDiasAtraso() {
        var creado = crear(usuario("501"), List.of(libro("Atrasado", 1)));

        assertThat(prestamoService.detectarAtrasados()).isEqualTo(1);
        assertThat(prestamoService.detectarAtrasados()).isZero();
        var atrasado = prestamoService.listar(0, 20, EstadoPrestamo.ATRASADO)
                .content().getFirst();
        assertThat(atrasado.id()).isEqualTo(creado.id());
        assertThat(atrasado.diasAtraso()).isPositive();
    }

    @Test
    void expiraSoloReservasPendientesConFechaVencidaYEsIdempotente() {
        var usuario = usuario("601");
        var vencida = libro("Reserva vencida", 1);
        var futura = libro("Reserva futura", 1);
        var sinExpiracion = libro("Reserva sin expiración", 1);
        var atendida = libro("Reserva atendida", 1);
        jdbcTemplate.update("""
                INSERT INTO reservas
                    (id_usuario, id_libro, fecha_reserva, fecha_expiracion, estado)
                VALUES
                    (?, ?, now(), now() - interval '1 day', 'PENDIENTE'),
                    (?, ?, now(), now() + interval '1 day', 'PENDIENTE'),
                    (?, ?, now(), null, 'PENDIENTE'),
                    (?, ?, now(), now() - interval '1 day', 'ATENDIDA')
                """, usuario, vencida, usuario, futura, usuario, sinExpiracion, usuario, atendida);

        assertThat(reservaService.expirarVencidas()).isEqualTo(1);
        assertThat(reservaService.expirarVencidas()).isZero();
        assertThat(estadoReserva(vencida)).isEqualTo("EXPIRADA");
        assertThat(estadoReserva(futura)).isEqualTo("PENDIENTE");
        assertThat(estadoReserva(sinExpiracion)).isEqualTo("PENDIENTE");
        assertThat(estadoReserva(atendida)).isEqualTo("ATENDIDA");
    }

    private String intentarCrear(long usuario, long libro) {
        try {
            crear(usuario, List.of(libro));
            return "CREADO";
        } catch (RuntimeException exception) {
            return exception.getClass().getSimpleName();
        }
    }

    private com.americana.bibliotecaserver.dto.PrestamoRespuesta crear(
            long usuario, List<Long> libros) {
        return prestamoService.crear(new CrearPrestamoSolicitud(
                usuario, LocalDate.of(2026, 9, 20), libros));
    }

    private long usuario(String cedula) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO usuarios (nombre, apellido, cedula, estado)
                VALUES ('Ana', 'Paz', ?, 'ACTIVO') RETURNING id_usuario
                """, Long.class, cedula);
    }

    private long libro(String titulo, int disponible) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO libros (titulo, cantidad_total, cantidad_disponible)
                VALUES (?, 1, ?) RETURNING id_libro
                """, Long.class, titulo, disponible);
    }

    private int stock(long libro) {
        return jdbcTemplate.queryForObject(
                "SELECT cantidad_disponible FROM libros WHERE id_libro = ?",
                Integer.class, libro);
    }

    private String estadoReserva(long libro) {
        return jdbcTemplate.queryForObject(
                "SELECT estado FROM reservas WHERE id_libro = ?", String.class, libro);
    }
}
