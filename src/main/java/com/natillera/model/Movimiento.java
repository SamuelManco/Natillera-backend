package com.natillera.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "tbl_movimiento")
@Data
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_natillera", nullable = false)
    private Long idNatillera;

    @Column(name = "id_persona")
    private Long idPersona;

    @Column(name = "id_tipo_movimiento", nullable = false)
    private Long idTipoMovimiento;

    @Column(name = "id_rifa")
    private Long idRifa;

    @Column(name = "id_movimiento_origen")
    private Long idMovimientoOrigen;

    @Column(name = "tasa_interes")
    private java.math.BigDecimal tasaInteres;

    @Column(name = "monto_actual")
    private Long montoActual;

    @Column(name = "monto", nullable = false)
    private Long monto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "descripcion")
    private String descripcion;

}
