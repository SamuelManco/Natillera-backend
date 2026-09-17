package com.natillera.dto;

import lombok.Data;

@Data
public class EstadoRifaResponse {
    private Long idRifa;
    private int boletasVendidas;
    private int boletasDisponibles;
    private Long totalRecaudado;
    private boolean finalizada;
    private String numeroGanador;
}
