package com.natillera.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AlertaPrestamoResponse {
    private Long idPrestamo;
    private Long idPersona;
    private String nombrePersona;
    private Long saldoPendiente;
    private LocalDate fechaLimite;   // último día en que se aceptan abonos
    private long diasRestantes;      // negativo si el plazo ya venció
    private String nivel;            // "PROXIMO" o "VENCIDO"
    private String mensaje;
}
