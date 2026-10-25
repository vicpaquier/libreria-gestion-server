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
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservas_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_libro", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservas_libro"))
    private Libro libro;

    @Column(name = "fecha_reserva", nullable = false, updatable = false)
    private OffsetDateTime fechaReserva;

    @Column(name = "fecha_expiracion")
    private OffsetDateTime fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado;

    protected Reserva() {
    }

    public Reserva(Usuario usuario, Libro libro, OffsetDateTime fechaReserva,
            OffsetDateTime fechaExpiracion) {
        this.usuario = usuario;
        this.libro = libro;
        this.fechaReserva = fechaReserva;
        this.fechaExpiracion = fechaExpiracion;
        this.estado = EstadoReserva.PENDIENTE;
    }

    public void atender() {
        estado = EstadoReserva.ATENDIDA;
    }

    public void cancelar() {
        estado = EstadoReserva.CANCELADA;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Libro getLibro() { return libro; }
    public OffsetDateTime getFechaReserva() { return fechaReserva; }
    public OffsetDateTime getFechaExpiracion() { return fechaExpiracion; }
    public EstadoReserva getEstado() { return estado; }
}
