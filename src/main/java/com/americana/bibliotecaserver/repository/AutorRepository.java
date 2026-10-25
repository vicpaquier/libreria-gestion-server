package com.americana.bibliotecaserver.repository;

import com.americana.bibliotecaserver.entity.Autor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AutorRepository extends JpaRepository<Autor, Long>,
        JpaSpecificationExecutor<Autor> {
}
