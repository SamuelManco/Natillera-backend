package com.natillera.model;

import jakarta.persistence.*;

import lombok.Data;

@Entity
@Table(name = "tbl_tipo_documento")
@Data
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

}
