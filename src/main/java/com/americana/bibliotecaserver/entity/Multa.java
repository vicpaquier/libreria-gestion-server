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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "multas")
public class Multa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_multa")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_prestamo", nullable = false,
            foreignKey = @ForeignKey(name = "fk_multas_prestamo"))
    private Prestamo prestamo;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "fecha_multa", nullable = false, updatable = false)
    private OffsetDateTime fechaMulta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMulta estado;

    protected Multa() {
    }

    public Multa(Prestamo prestamo, BigDecimal monto, String motivo, OffsetDateTime fechaMulta) {
        this.prestamo = prestamo;
        this.monto = monto;
        this.motivo = motivo;
        this.fechaMulta = fechaMulta;
        this.estado = EstadoMulta.PENDIENTE;
    }

    public void pagar() {
        estado = EstadoMulta.PAGADA;
    }

    public void cancelar() {
        estado = EstadoMulta.CANCELADA;
    }

    public Long getId() { return id; }
    public Prestamo getPrestamo() { return prestamo; }
    public BigDecimal getMonto() { return monto; }
    public String getMotivo() { return motivo; }
    public OffsetDateTime getFechaMulta() { return fechaMulta; }
    public EstadoMulta getEstado() { return estado; }
}
