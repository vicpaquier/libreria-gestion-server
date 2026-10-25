package com.americana.bibliotecaserver.repository;

import com.americana.bibliotecaserver.entity.EstadoPrestamo;
import com.americana.bibliotecaserver.entity.EstadoDetallePrestamo;
import com.americana.bibliotecaserver.entity.Prestamo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    Page<Prestamo> findByEstado(EstadoPrestamo estado, Pageable pageable);

    long countByEstado(EstadoPrestamo estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Prestamo p where p.id = :id")
    Optional<Prestamo> buscarPorIdConBloqueo(@Param("id") long id);

    @EntityGraph(attributePaths = {"usuario", "detalles", "detalles.libro"})
    @Query("select distinct p from Prestamo p where p.id in :ids order by p.id asc")
    List<Prestamo> buscarTodosConDetalles(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"usuario", "detalles", "detalles.libro"})
    @Query("select distinct p from Prestamo p where p.usuario.id = :usuarioId order by p.id asc")
    List<Prestamo> buscarPorUsuarioConDetalles(@Param("usuarioId") long usuarioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Prestamo p set p.estado = :atrasado
            where p.estado = :activo and p.fechaVencimiento < :fecha
            and exists (select d.id from DetallePrestamo d
                        where d.prestamo = p and d.estado = :prestado)
            """)
    int marcarAtrasados(@Param("fecha") LocalDate fecha,
            @Param("activo") EstadoPrestamo activo,
            @Param("atrasado") EstadoPrestamo atrasado,
            @Param("prestado") EstadoDetallePrestamo prestado);

    @EntityGraph(attributePaths = "usuario")
    @Query("""
            select distinct p from Prestamo p join p.detalles d
            where d.libro.id = :libroId and d.estado = :estadoDetalle
            order by p.id asc
            """)
    List<Prestamo> buscarPorLibroYEstadoDetalle(
            @Param("libroId") long libroId,
            @Param("estadoDetalle") EstadoDetallePrestamo estadoDetalle);
}
