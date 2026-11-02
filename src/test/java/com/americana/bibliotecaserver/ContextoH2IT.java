package com.americana.bibliotecaserver;

import com.americana.bibliotecaserver.repository.AutorRepository;
import com.americana.bibliotecaserver.repository.CategoriaRepository;
import com.americana.bibliotecaserver.repository.EditorialRepository;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class ContextoH2IT {

    @Autowired
    private AutorRepository autorRepository;

    @Autowired
    private EditorialRepository editorialRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void iniciaConDatosMinimosEnH2() {
        assertThat(autorRepository.count()).isEqualTo(3);
        assertThat(editorialRepository.count()).isEqualTo(2);
        assertThat(categoriaRepository.count()).isEqualTo(2);
        assertThat(libroRepository.count()).isEqualTo(3);
        assertThat(usuarioRepository.count()).isEqualTo(2);
        assertThat(usuarioRepository.existsByCedula("12345678")).isTrue();
        assertThat(libroRepository.existsByIsbn("978-84-376-0494-7")).isTrue();
    }
}
