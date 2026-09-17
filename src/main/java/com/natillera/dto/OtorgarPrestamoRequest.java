package com.natillera.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OtorgarPrestamoRequest {
    private Long idNatillera;
    private Long idPersona;
    private Long monto;
    private java.math.BigDecimal tasaInteres; // ej. 2.00 significa 2% mensual
    private LocalDate fecha;
}
