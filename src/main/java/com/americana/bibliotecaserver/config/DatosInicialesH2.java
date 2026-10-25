package com.americana.bibliotecaserver.config;

import com.americana.bibliotecaserver.entity.Autor;
import com.americana.bibliotecaserver.entity.Categoria;
import com.americana.bibliotecaserver.entity.Editorial;
import com.americana.bibliotecaserver.entity.Libro;
import com.americana.bibliotecaserver.entity.Usuario;
import com.americana.bibliotecaserver.repository.AutorRepository;
import com.americana.bibliotecaserver.repository.CategoriaRepository;
import com.americana.bibliotecaserver.repository.EditorialRepository;
import com.americana.bibliotecaserver.repository.LibroRepository;
import com.americana.bibliotecaserver.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("h2")
public class DatosInicialesH2 implements ApplicationRunner {

    private final AutorRepository autorRepository;
    private final EditorialRepository editorialRepository;
    private final CategoriaRepository categoriaRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;

    public DatosInicialesH2(AutorRepository autorRepository,
            EditorialRepository editorialRepository,
            CategoriaRepository categoriaRepository,
            LibroRepository libroRepository,
            UsuarioRepository usuarioRepository) {
        this.autorRepository = autorRepository;
        this.editorialRepository = editorialRepository;
        this.categoriaRepository = categoriaRepository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0 || libroRepository.count() > 0) {
            return;
        }

        var garciaMarquez = autorRepository.save(
                new Autor("Gabriel", "García Márquez", "Colombiana"));
        var allende = autorRepository.save(new Autor("Isabel", "Allende", "Chilena"));
        var cortazar = autorRepository.save(new Autor("Julio", "Cortázar", "Argentina"));

        var sudamericana = editorialRepository.save(
                new Editorial("Editorial Sudamericana", "Argentina"));
        var penguin = editorialRepository.save(
                new Editorial("Penguin Random House", "España"));

        var novela = categoriaRepository.save(
                new Categoria("Novela", "Narrativa extensa"));
        var cuento = categoriaRepository.save(
                new Categoria("Cuento", "Narrativa breve"));

        libroRepository.saveAll(List.of(
                new Libro("Cien años de soledad", "978-84-376-0494-7", 1967, 471, 3,
                        sudamericana, List.of(garciaMarquez), List.of(novela)),
                new Libro("La casa de los espíritus", "978-84-01-01329-8", 1982, 433, 2,
                        penguin, List.of(allende), List.of(novela)),
                new Libro("Bestiario", "978-84-376-0742-9", 1951, 168, 2,
                        sudamericana, List.of(cortazar), List.of(cuento))));

        usuarioRepository.saveAll(List.of(
                new Usuario("Ana", "Pérez", "12345678", "ana.perez@example.com",
                        "3001234567", "Calle 1 #2-3"),
                new Usuario("Carlos", "Gómez", "87654321", "carlos.gomez@example.com",
                        "3007654321", "Carrera 4 #5-6")));
    }
}
