package com.natillera.model;

import jakarta.persistence.*;

import lombok.Data;

@Entity
@Table(name = "tbl_estado")
@Data
public class Estado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "id_categoria", nullable = false)
    private Long idCategoria;

}
