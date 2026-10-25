package com.americana.bibliotecaserver.repository;

import com.americana.bibliotecaserver.entity.Libro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long>,
        JpaSpecificationExecutor<Libro> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, long id);

    @Query("select coalesce(sum(l.cantidadDisponible), 0) from Libro l")
    long sumarCantidadDisponible();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id = :id")
    Optional<Libro> buscarPorIdConBloqueo(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Libro l where l.id in :ids order by l.id asc")
    List<Libro> buscarTodosPorIdConBloqueo(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"editorial", "autores", "categorias"})
    @Query("select distinct l from Libro l where l.id in :ids order by l.id asc")
    List<Libro> buscarTodosConRelaciones(@Param("ids") List<Long> ids);
}
