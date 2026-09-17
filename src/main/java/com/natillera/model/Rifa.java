package com.natillera.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "tbl_rifa")
@Data
public class Rifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_natillera", nullable = false)
    private Long idNatillera;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "precio_boleta", nullable = false)
    private Long precioBoleta;

    @Column(name = "cantidad_boletas", nullable = false)
    private Integer cantidadBoletas;

    @Column(name = "numero_ganador")
    private String numeroGanador;

    @Column(name = "id_estado", nullable = false)
    private Long idEstado;

}
