package com.natillera.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class VenderBoletaRequest {
    private String numero;
    private Long idPersona;
    private LocalDate fecha;
}
