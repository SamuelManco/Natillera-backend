package com.natillera.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PagarLiquidacionRequest {
    private LocalDate fecha;
}
