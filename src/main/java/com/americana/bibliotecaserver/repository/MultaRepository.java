package com.americana.bibliotecaserver.repository;

import com.americana.bibliotecaserver.entity.EstadoMulta;
import com.americana.bibliotecaserver.entity.Multa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MultaRepository extends JpaRepository<Multa, Long>, JpaSpecificationExecutor<Multa> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Multa m where m.id = :id")
    Optional<Multa> buscarPorIdConBloqueo(@Param("id") long id);

    @EntityGraph(attributePaths = "prestamo")
    List<Multa> findByPrestamoUsuarioIdOrderByIdAsc(long usuarioId);

    @EntityGraph(attributePaths = "prestamo")
    List<Multa> findByPrestamoIdInOrderByPrestamoIdAscIdAsc(List<Long> prestamosIds);

    @Query("select coalesce(sum(m.monto), 0) from Multa m where m.estado = :estado")
    BigDecimal sumarMontoPorEstado(@Param("estado") EstadoMulta estado);
}
