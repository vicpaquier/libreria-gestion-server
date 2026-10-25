package com.americana.bibliotecaserver;

import com.americana.bibliotecaserver.dto.CrearLibroSolicitud;
import com.americana.bibliotecaserver.exception.ConflictoNegocioException;
import com.americana.bibliotecaserver.exception.ConflictoUnicidadException;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.service.LibroService;
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

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers(disabledWithoutDocker = true)
class LibroPersistenciaIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired LibroService libroService;
    @Autowired LibroRepository libroRepository;

    @BeforeEach
    void limpiar() {
        jdbcTemplate.update("DELETE FROM detalle_prestamo");
        jdbcTemplate.update("DELETE FROM prestamos");
        jdbcTemplate.update("DELETE FROM libros");
        jdbcTemplate.update("DELETE FROM usuarios");
        jdbcTemplate.update("DELETE FROM autores");
        jdbcTemplate.update("DELETE FROM categorias");
        jdbcTemplate.update("DELETE FROM editoriales");
    }

    @Test
    void aplicaV5ConConstraintsIndicesYAccionesDeFk() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '5'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForList("""
                SELECT conname FROM pg_constraint
                WHERE conname IN ('uq_libros_isbn', 'ck_libros_cantidad_total',
                    'ck_libros_cantidad_disponible', 'ck_libros_disponible_total',
                    'fk_libros_editorial', 'pk_libro_autor', 'fk_libro_autor_libro',
                    'fk_libro_autor_autor', 'pk_libro_categoria',
                    'fk_libro_categoria_libro', 'fk_libro_categoria_categoria')
                """, String.class)).hasSize(11);
        assertThat(jdbcTemplate.queryForList("""
                SELECT indexname FROM pg_indexes
                WHERE indexname IN ('idx_libros_titulo', 'idx_libros_isbn')
                """, String.class)).containsExactlyInAnyOrder("idx_libros_titulo", "idx_libros_isbn");
        assertThat(jdbcTemplate.queryForMap("""
                SELECT confdeltype, confupdtype FROM pg_constraint
                WHERE conname = 'fk_libros_editorial'
                """)).containsEntry("confdeltype", "n").containsEntry("confupdtype", "c");
        assertThat(jdbcTemplate.queryForList("""
                SELECT confdeltype::text || confupdtype::text FROM pg_constraint
                WHERE conname IN ('fk_libro_autor_libro', 'fk_libro_autor_autor',
                    'fk_libro_categoria_libro', 'fk_libro_categoria_categoria')
                """, String.class)).containsOnly("cc");
    }

    @Test
    void creaRelacionesDeterministasPermiteVariosIsbnNulosYConsultaDetalleVacio() {
        var referencias = referencias();
        var primero = crear("Libro", "  ", 5, referencias);
        var segundo = crear("Otro", null, 1, referencias);

        assertThat(primero.cantidadDisponible()).isEqualTo(5);
        assertThat(primero.isbn()).isNull();
        assertThat(segundo.isbn()).isNull();
        assertThat(primero.autores()).extracting("id")
                .containsExactly(referencias.autor1(), referencias.autor2());
        var detalle = libroService.consultar(primero.id());
        assertThat(detalle.prestamosActivos()).isEmpty();
        assertThat(detalle.reservasPendientes()).isEmpty();
    }

    @Test
    void buscaYFiltraEnDbConComodinesLiteralesYPaginacionEstable() {
        var referencias = referencias();
        var porcentaje = crear("Código%Real", "UNO", 1, referencias);
        crear("CódigoXReal", "DOS", 0, referencias);

        assertThat(libroService.listar(0, 20, "%", null, null, null, null, null).content())
                .extracting("id").containsExactly(porcentaje.id());
        assertThat(libroService.listar(0, 1, null, referencias.autor1(),
                referencias.categoria(), referencias.editorial(), 2000, true).content())
                .extracting("id").containsExactly(porcentaje.id());
        assertThat(libroService.listar(99, 1, null, null, null, null, null, null).content()).isEmpty();
    }

    @Test
    void busquedaGeneralEsIndependienteDeFiltrosConVariasRelaciones() {
        var referencias = referencias();
        var libro = crear("Sin coincidencia", "ISBN-MULTIPLE", 1, referencias);
        var otraCategoria = jdbcTemplate.queryForObject("""
                INSERT INTO categorias (nombre) VALUES ('Otra categoría') RETURNING id_categoria
                """, Long.class);
        jdbcTemplate.update("""
                INSERT INTO libro_categoria (id_libro, id_categoria) VALUES (?, ?)
                """, libro.id(), otraCategoria);

        assertThat(libroService.listar(0, 20, "Luis Rojas", referencias.autor1(),
                null, null, null, null).content()).extracting("id").containsExactly(libro.id());
        assertThat(libroService.listar(0, 20, "Otra categoría", null,
                referencias.categoria(), null, null, null).content())
                .extracting("id").containsExactly(libro.id());
    }

    @Test
    void aplicaSetNullYCascadasDeJoins() {
        var referencias = referencias();
        var libro = crear("Libro", "ISBN-CASCADE", 1, referencias);

        jdbcTemplate.update("DELETE FROM editoriales WHERE id_editorial = ?", referencias.editorial());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT id_editorial FROM libros WHERE id_libro = ?", Long.class, libro.id())).isNull();

        jdbcTemplate.update("DELETE FROM autores WHERE id_autor = ?", referencias.autor1());
        assertThat(conteo("libro_autor", libro.id())).isEqualTo(1);
        jdbcTemplate.update("DELETE FROM categorias WHERE id_categoria = ?", referencias.categoria());
        assertThat(conteo("libro_categoria", libro.id())).isZero();

        jdbcTemplate.update("DELETE FROM libros WHERE id_libro = ?", libro.id());
        assertThat(conteo("libro_autor", libro.id())).isZero();
    }

    @Test
    void preservaNoDisponiblesConBloqueoYRechazaReduccion() {
        var referencias = referencias();
        var libro = crear("Stock", "ISBN-STOCK", 5, referencias);
        jdbcTemplate.update("UPDATE libros SET cantidad_disponible = 2 WHERE id_libro = ?", libro.id());

        var ajustado = libroService.ajustarStock(libro.id(), 7);
        assertThat(ajustado.cantidadTotal()).isEqualTo(7);
        assertThat(ajustado.cantidadDisponible()).isEqualTo(4);
        assertThatThrownBy(() -> libroService.ajustarStock(libro.id(), 2))
                .isInstanceOf(ConflictoNegocioException.class);
    }

    @Test
    void detalleDevuelveSoloPrestamosConDetallePrestadoEnOrden() {
        var referencias = referencias();
        var libro = crear("Prestado", "ISBN-PRESTADO", 2, referencias);
        var usuario = jdbcTemplate.queryForObject("""
                INSERT INTO usuarios (nombre, apellido, cedula, estado)
                VALUES ('Ana', 'Paz', '900', 'ACTIVO') RETURNING id_usuario
                """, Long.class);
        var activo = prestamo(usuario, "ACTIVO");
        var devuelto = prestamo(usuario, "DEVUELTO");
        jdbcTemplate.update("""
                INSERT INTO detalle_prestamo (id_prestamo, id_libro, estado)
                VALUES (?, ?, 'PRESTADO'), (?, ?, 'DEVUELTO')
                """, activo, libro.id(), devuelto, libro.id());

        var detalle = libroService.consultar(libro.id());

        assertThat(detalle.prestamosActivos()).singleElement().satisfies(prestamo -> {
            assertThat(prestamo.id()).isEqualTo(activo);
            assertThat(prestamo.usuarioId()).isEqualTo(usuario);
            assertThat(prestamo.estado().name()).isEqualTo("ACTIVO");
        });
        assertThat(detalle.reservasPendientes()).isEmpty();
    }

    @Test
    void dosCreacionesConcurrentesDelMismoIsbnDejanUnaYUnConflicto() throws Exception {
        var referencias = referencias();
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            var primera = ejecutor.submit(() -> intentarCrear("A", referencias));
            var segunda = ejecutor.submit(() -> intentarCrear("B", referencias));
            assertThat(List.of(primera.get(), segunda.get()))
                    .containsExactlyInAnyOrder("CREADO", ConflictoUnicidadException.class.getSimpleName());
        }
        assertThat(libroRepository.count()).isEqualTo(1);
    }

    private String intentarCrear(String titulo, Referencias referencias) {
        try {
            crear(titulo, "ISBN-UNICO", 1, referencias);
            return "CREADO";
        } catch (RuntimeException exception) {
            return exception.getClass().getSimpleName();
        }
    }

    private com.americana.bibliotecaserver.dto.LibroRespuesta crear(
            String titulo, String isbn, int total, Referencias referencias) {
        return libroService.crear(new CrearLibroSolicitud(titulo, isbn, 2000, 100, total,
                referencias.editorial(), List.of(referencias.autor2(), referencias.autor1()),
                List.of(referencias.categoria())));
    }

    private Referencias referencias() {
        var autor1 = jdbcTemplate.queryForObject("""
                INSERT INTO autores (nombre, apellido) VALUES ('Ana', 'Paz') RETURNING id_autor
                """, Long.class);
        var autor2 = jdbcTemplate.queryForObject("""
                INSERT INTO autores (nombre, apellido) VALUES ('Luis', 'Rojas') RETURNING id_autor
                """, Long.class);
        var categoria = jdbcTemplate.queryForObject("""
                INSERT INTO categorias (nombre) VALUES ('Categoría') RETURNING id_categoria
                """, Long.class);
        var editorial = jdbcTemplate.queryForObject("""
                INSERT INTO editoriales (nombre) VALUES ('Editorial') RETURNING id_editorial
                """, Long.class);
        return new Referencias(autor1, autor2, categoria, editorial);
    }

    private int conteo(String tabla, long libroId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + tabla + " WHERE id_libro = ?", Integer.class, libroId);
    }

    private long prestamo(long usuario, String estado) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO prestamos
                    (id_usuario, fecha_prestamo, fecha_vencimiento, estado)
                VALUES (?, '2026-09-09T10:00:00Z', '2026-09-20', ?)
                RETURNING id_prestamo
                """, Long.class, usuario, estado);
    }

    private record Referencias(long autor1, long autor2, long categoria, long editorial) { }
}
