package com.natillera.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CerrarLiquidacionRequest {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
