package com.americana.bibliotecaserver.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "prestamos")
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prestamo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false,
            foreignKey = @ForeignKey(name = "fk_prestamos_usuario"))
    private Usuario usuario;

    @Column(name = "fecha_prestamo", nullable = false, updatable = false)
    private OffsetDateTime fechaPrestamo;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPrestamo estado;

    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetallePrestamo> detalles = new ArrayList<>();

    protected Prestamo() {
    }

    public Prestamo(Usuario usuario, OffsetDateTime fechaPrestamo, LocalDate fechaVencimiento,
            Collection<Libro> libros) {
        this.usuario = usuario;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = EstadoPrestamo.ACTIVO;
        libros.forEach(libro -> detalles.add(new DetallePrestamo(this, libro)));
    }

    public void finalizarSiNoHayPrestados() {
        if (detalles.stream().noneMatch(detalle ->
                detalle.getEstado() == EstadoDetallePrestamo.PRESTADO)) {
            estado = EstadoPrestamo.DEVUELTO;
        }
    }

    public void marcarAtrasado() {
        if (estado == EstadoPrestamo.ACTIVO
                && detalles.stream().anyMatch(detalle ->
                        detalle.getEstado() == EstadoDetallePrestamo.PRESTADO)) {
            estado = EstadoPrestamo.ATRASADO;
        }
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public OffsetDateTime getFechaPrestamo() { return fechaPrestamo; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public EstadoPrestamo getEstado() { return estado; }
    public List<DetallePrestamo> getDetalles() { return detalles; }
}
