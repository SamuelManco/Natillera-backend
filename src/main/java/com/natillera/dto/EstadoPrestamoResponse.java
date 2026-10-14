package com.natillera.dto;

import com.natillera.model.Movimiento;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EstadoPrestamoResponse {
    private Long idPrestamo;
    private Long montoOriginal;
    private Long montoActual;         // lo que falta por pagar
    private Long totalAbonadoCapital;
    private Long totalInteresPagado;
    private boolean pagadoCompleto;
    private LocalDate fechaLimiteAbonos; // último día en que se aceptan abonos
    private List<Movimiento> historial;  // todos los abonos e intereses de este préstamo
}
