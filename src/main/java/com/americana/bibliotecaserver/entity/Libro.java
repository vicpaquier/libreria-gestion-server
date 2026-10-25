package com.americana.bibliotecaserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "libros", uniqueConstraints =
        @UniqueConstraint(name = "uq_libros_isbn", columnNames = "isbn"))
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_libro")
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private String isbn;

    @Column(name = "anio_publicacion")
    private Integer anioPublicacion;

    @Column(name = "numero_paginas")
    private Integer numeroPaginas;

    @Column(name = "cantidad_total", nullable = false)
    private int cantidadTotal;

    @Column(name = "cantidad_disponible", nullable = false)
    private int cantidadDisponible;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_editorial", foreignKey = @ForeignKey(name = "fk_libros_editorial"))
    private Editorial editorial;

    @ManyToMany
    @JoinTable(name = "libro_autor",
            joinColumns = @JoinColumn(name = "id_libro",
                    foreignKey = @ForeignKey(name = "fk_libro_autor_libro")),
            inverseJoinColumns = @JoinColumn(name = "id_autor",
                    foreignKey = @ForeignKey(name = "fk_libro_autor_autor")))
    @OrderBy("id ASC")
    private Set<Autor> autores = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "libro_categoria",
            joinColumns = @JoinColumn(name = "id_libro",
                    foreignKey = @ForeignKey(name = "fk_libro_categoria_libro")),
            inverseJoinColumns = @JoinColumn(name = "id_categoria",
                    foreignKey = @ForeignKey(name = "fk_libro_categoria_categoria")))
    @OrderBy("id ASC")
    private Set<Categoria> categorias = new LinkedHashSet<>();

    protected Libro() {
    }

    public Libro(String titulo, String isbn, Integer anioPublicacion, Integer numeroPaginas,
            int cantidadTotal, Editorial editorial, Collection<Autor> autores,
            Collection<Categoria> categorias) {
        this.titulo = titulo;
        this.isbn = isbn;
        this.anioPublicacion = anioPublicacion;
        this.numeroPaginas = numeroPaginas;
        this.cantidadTotal = cantidadTotal;
        this.cantidadDisponible = cantidadTotal;
        this.editorial = editorial;
        this.autores.addAll(autores);
        this.categorias.addAll(categorias);
    }

    public void actualizar(String titulo, String isbn, Integer anioPublicacion,
            Integer numeroPaginas, Editorial editorial, Collection<Autor> autores,
            Collection<Categoria> categorias) {
        this.titulo = titulo;
        this.isbn = isbn;
        this.anioPublicacion = anioPublicacion;
        this.numeroPaginas = numeroPaginas;
        this.editorial = editorial;
        this.autores.clear();
        this.autores.addAll(autores);
        this.categorias.clear();
        this.categorias.addAll(categorias);
    }

    public void ajustarCantidadTotal(int nuevaCantidadTotal) {
        int unidadesNoDisponibles = cantidadTotal - cantidadDisponible;
        this.cantidadTotal = nuevaCantidadTotal;
        this.cantidadDisponible = nuevaCantidadTotal - unidadesNoDisponibles;
    }

    public void prestarUnidad() {
        cantidadDisponible--;
    }

    public void devolverUnidad() {
        cantidadDisponible++;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getIsbn() { return isbn; }
    public Integer getAnioPublicacion() { return anioPublicacion; }
    public Integer getNumeroPaginas() { return numeroPaginas; }
    public int getCantidadTotal() { return cantidadTotal; }
    public int getCantidadDisponible() { return cantidadDisponible; }
    public Editorial getEditorial() { return editorial; }
    public Set<Autor> getAutores() { return autores; }
    public Set<Categoria> getCategorias() { return categorias; }
}
