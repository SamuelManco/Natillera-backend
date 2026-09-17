package com.natillera.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "tbl_natillera")
@Data
public class Natillera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "id_estado", nullable = false)
    private Long idEstado;

    @Column(name = "total_ingreso_rifa")
    private Long totalIngresoRifa;

    @Column(name = "total_intereses_prestamos")
    private Long totalInteresesPrestamos;

    @Column(name = "total_intereses")
    private Long totalIntereses;

}
