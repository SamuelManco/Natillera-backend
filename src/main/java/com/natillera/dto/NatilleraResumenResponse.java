package com.natillera.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class NatilleraResumenResponse {
    private Long idNatillera;
    private String nombreNatillera;
    private LocalDate desde;   // filtro opcional
    private LocalDate hasta;   // filtro opcional
    private boolean periodoActual;   // true si los totales empiezan después del último cierre
    private LocalDate ultimoCierre;  // fecha fin del último periodo liquidado (null si no hay)

    // Ahorro de los socios
    private long totalAportes;
    private long totalPremiosRifa;     // premios de rifa: se suman al ahorro del ganador
    private long totalAhorrado;        // aportes + premios de rifa

    // Fondo de intereses (lo que se reparte al liquidar)
    private long interesesPrestamos;
    private long ingresosRifa;         // rifas en las que nadie ganó
    private long totalIntereses;       // interesesPrestamos + ingresosRifa

    // Capital y caja
    private long totalCapital;         // totalAhorrado + totalIntereses
    private long prestamosPorCobrar;   // saldo pendiente de los préstamos activos
    private int prestamosActivos;
    private long pagosLiquidacion;     // lo ya entregado a los socios
    private long disponibleEnCaja;     // saldo de todo el tiempo: lo recibido - préstamos por cobrar - pagos entregados

    private int numeroSocios;
    private List<AhorroSocio> ahorroPorSocio;

    @Data
    public static class AhorroSocio {
        private Long idPersona;
        private String nombre;
        private long ahorrado;
        private Long cuotaMinima;
    }
}
