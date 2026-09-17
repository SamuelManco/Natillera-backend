package com.natillera.model;

import jakarta.persistence.*;

import lombok.Data;

@Entity
@Table(name = "tbl_liquidacionxpersona")
@Data
public class LiquidacionxPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_persona", nullable = false)
    private Long idPersona;

    @Column(name = "id_liquidacion", nullable = false)
    private Long idLiquidacion;

    @Column(name = "monto_ahorrado", nullable = false)
    private Long montoAhorrado;

    @Column(name = "monto_intereses", nullable = false)
    private Long montoIntereses;

    @Column(name = "porcentaje", nullable = false)
    private java.math.BigDecimal porcentaje;

    @Column(name = "monto_total", nullable = false)
    private Long montoTotal;

    @Column(name = "id_estado", nullable = false)
    private Long idEstado;

    @Column(name = "id_movimiento_pago")
    private Long idMovimientoPago;

}
