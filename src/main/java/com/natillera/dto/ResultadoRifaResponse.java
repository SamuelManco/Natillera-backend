package com.natillera.dto;

import lombok.Data;

@Data
public class ResultadoRifaResponse {
    private Long idRifa;
    private String numeroGanador;
    private Long idBoletaGanadora;
    private Long idPersonaGanadora;
    private Long totalRecaudado;
}
