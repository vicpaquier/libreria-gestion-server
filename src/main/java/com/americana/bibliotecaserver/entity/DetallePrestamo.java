package com.americana.bibliotecaserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "detalle_prestamo")
public class DetallePrestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_prestamo", nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_prestamo_prestamo"))
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_libro", nullable = false,
            foreignKey = @ForeignKey(name = "fk_detalle_prestamo_libro"))
    private Libro libro;

    @Column(name = "fecha_devolucion")
    private OffsetDateTime fechaDevolucion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDetallePrestamo estado;

    protected DetallePrestamo() {
    }

    DetallePrestamo(Prestamo prestamo, Libro libro) {
        this.prestamo = prestamo;
        this.libro = libro;
        this.estado = EstadoDetallePrestamo.PRESTADO;
    }

    public void devolver(OffsetDateTime fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
        this.estado = EstadoDetallePrestamo.DEVUELTO;
    }

    public void marcarPerdido() {
        this.estado = EstadoDetallePrestamo.PERDIDO;
    }

    public Long getId() { return id; }
    public Prestamo getPrestamo() { return prestamo; }
    public Libro getLibro() { return libro; }
    public OffsetDateTime getFechaDevolucion() { return fechaDevolucion; }
    public EstadoDetallePrestamo getEstado() { return estado; }
}
