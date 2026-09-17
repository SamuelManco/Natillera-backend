package com.natillera.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "tbl_boleta")
@Data
public class Boleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_rifa", nullable = false)
    private Long idRifa;

    @Column(name = "numero", nullable = false)
    private String numero;

    @Column(name = "id_persona")
    private Long idPersona;

    @Column(name = "fecha_compra")
    private LocalDate fechaCompra;

    @Column(name = "id_estado", nullable = false)
    private Long idEstado;

}
