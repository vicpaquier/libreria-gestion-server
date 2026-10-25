package com.americana.bibliotecaserver.repository;

import com.americana.bibliotecaserver.entity.EstadoReserva;
import com.americana.bibliotecaserver.entity.Reserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByUsuarioIdAndLibroIdAndEstado(long usuarioId, long libroId, EstadoReserva estado);

    long countByEstado(EstadoReserva estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Reserva r set r.estado = :expirada
            where r.estado = :pendiente and r.fechaExpiracion is not null
            and r.fechaExpiracion < :ahora
            """)
    int marcarExpiradas(@Param("ahora") OffsetDateTime ahora,
            @Param("pendiente") EstadoReserva pendiente,
            @Param("expirada") EstadoReserva expirada);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> buscarPorIdConBloqueo(@Param("id") long id);

    @EntityGraph(attributePaths = "libro")
    List<Reserva> findByLibroIdAndEstadoOrderByIdAsc(long libroId, EstadoReserva estado);

    @EntityGraph(attributePaths = "libro")
    List<Reserva> findByUsuarioIdOrderByIdAsc(long usuarioId);
}
