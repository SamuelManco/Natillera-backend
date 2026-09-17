package com.natillera.dto;

import lombok.Data;
import java.util.List;
import com.natillera.model.Movimiento;

@Data
public class EstadoPrestamoResponse {
    private Long idPrestamo;
    private Long montoOriginal;
    private Long montoActual;         // lo que falta por pagar
    private Long totalAbonadoCapital;
    private Long totalInteresPagado;
    private boolean pagadoCompleto;
    private List<Movimiento> historial; // todos los abonos e intereses de este préstamo
}
