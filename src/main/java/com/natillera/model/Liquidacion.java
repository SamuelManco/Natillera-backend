package com.natillera.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "tbl_liquidacion")
@Data
public class Liquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_natillera", nullable = false)
    private Long idNatillera;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "id_estado", nullable = false)
    private Long idEstado;

    @Column(name = "total_intereses_prestamos")
    private Long totalInteresesPrestamos;

    @Column(name = "total_ingreso_rifa")
    private Long totalIngresoRifa;

    @Column(name = "total_intereses")
    private Long totalIntereses;

}
