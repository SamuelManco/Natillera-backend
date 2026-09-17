package com.natillera.service;

import com.natillera.dto.CerrarLiquidacionRequest;
import com.natillera.dto.LiquidacionResumenResponse;
import com.natillera.dto.PagarLiquidacionRequest;
import com.natillera.model.Estado;
import com.natillera.model.Liquidacion;
import com.natillera.model.LiquidacionxPersona;
import com.natillera.model.Movimiento;
import com.natillera.model.TipoMovimiento;
import com.natillera.repository.EstadoRepository;
import com.natillera.repository.LiquidacionRepository;
import com.natillera.repository.LiquidacionxPersonaRepository;
import com.natillera.repository.MovimientoRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LiquidacionService {

    // Nombres exactos que deben existir en Tbl_Tipo_Movimiento (créalos primero vía /api/tipo-movimiento)
    private static final String TIPO_APORTE = "Aporte";
    private static final String TIPO_INTERES_PRESTAMO = "Interés Préstamo";
    private static final String TIPO_INGRESO_RIFA = "Ingreso Rifa";
    private static final String TIPO_PAGO_LIQUIDACION = "Pago Liquidación";

    // Nombres exactos que deben existir en Tbl_Estado (créalos primero vía /api/estado)
    private static final String ESTADO_LIQUIDACION_CERRADA = "Cerrada";
    private static final String ESTADO_LIQUIDACIONXPERSONA_PENDIENTE = "Pendiente";
    private static final String ESTADO_LIQUIDACIONXPERSONA_PAGADA = "Pagada";

    @Autowired
    private LiquidacionRepository liquidacionRepository;

    @Autowired
    private LiquidacionxPersonaRepository liquidacionxPersonaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    private TipoMovimiento tipoPorNombre(String nombre) {
        return tipoMovimientoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el Tipo_Movimiento '" + nombre + "'. Créalo primero en /api/tipo-movimiento"));
    }

    private Estado estadoPorNombre(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el Estado '" + nombre + "'. Créalo primero en /api/estado"));
    }

    private long sumarMonto(List<Movimiento> movimientos) {
        return movimientos.stream().mapToLong(Movimiento::getMonto).sum();
    }

    // ---------- Calcular y cerrar la liquidación de un periodo ----------
    // TODO se calcula SOLO con movimientos dentro de fechaInicio-fechaFin del periodo,
    // nunca con el acumulado histórico de Natillera -- así un segundo periodo no repite
    // dinero ya repartido en el primero.
    @Transactional
    public LiquidacionResumenResponse cerrarLiquidacion(Long idNatillera, CerrarLiquidacionRequest request) {
        TipoMovimiento tipoAporte = tipoPorNombre(TIPO_APORTE);
        TipoMovimiento tipoInteresPrestamo = tipoPorNombre(TIPO_INTERES_PRESTAMO);
        TipoMovimiento tipoIngresoRifa = tipoPorNombre(TIPO_INGRESO_RIFA);

        // 1. Ahorro de cada persona, SOLO dentro de este periodo
        List<Movimiento> aportes = movimientoRepository.findByIdNatilleraAndIdTipoMovimientoAndFechaBetween(
                idNatillera, tipoAporte.getId(), request.getFechaInicio(), request.getFechaFin());
        Map<Long, Long> ahorroPorPersona = aportes.stream()
                .collect(Collectors.groupingBy(Movimiento::getIdPersona, Collectors.summingLong(Movimiento::getMonto)));

        if (ahorroPorPersona.isEmpty()) {
            throw new IllegalStateException("No hay aportes registrados en ese rango de fechas para liquidar");
        }
        long totalAhorrado = ahorroPorPersona.values().stream().mapToLong(Long::longValue).sum();

        // 2. Fondo a repartir, SOLO lo generado dentro de este periodo (no el histórico de Natillera)
        long totalInteresesPrestamos = sumarMonto(movimientoRepository.findByIdNatilleraAndIdTipoMovimientoAndFechaBetween(
                idNatillera, tipoInteresPrestamo.getId(), request.getFechaInicio(), request.getFechaFin()));
        long totalIngresoRifa = sumarMonto(movimientoRepository.findByIdNatilleraAndIdTipoMovimientoAndFechaBetween(
                idNatillera, tipoIngresoRifa.getId(), request.getFechaInicio(), request.getFechaFin()));
        long totalInteresesRepartir = totalInteresesPrestamos + totalIngresoRifa;

        // 3. Encabezado de la liquidación (snapshot del periodo)
        Estado estadoCerrada = estadoPorNombre(ESTADO_LIQUIDACION_CERRADA);
        Liquidacion liquidacion = new Liquidacion();
        liquidacion.setIdNatillera(idNatillera);
        liquidacion.setFechaInicio(request.getFechaInicio());
        liquidacion.setFechaFin(request.getFechaFin());
        liquidacion.setIdEstado(estadoCerrada.getId());
        liquidacion.setTotalInteresesPrestamos(totalInteresesPrestamos);
        liquidacion.setTotalIngresoRifa(totalIngresoRifa);
        liquidacion.setTotalIntereses(totalInteresesRepartir);
        liquidacion = liquidacionRepository.save(liquidacion);

        // 4. Reparto proporcional por persona
        Estado estadoPendiente = estadoPorNombre(ESTADO_LIQUIDACIONXPERSONA_PENDIENTE);
        for (Map.Entry<Long, Long> entry : ahorroPorPersona.entrySet()) {
            long montoAhorrado = entry.getValue();

            BigDecimal porcentaje = BigDecimal.valueOf(montoAhorrado)
                    .divide(BigDecimal.valueOf(totalAhorrado), 6, RoundingMode.HALF_UP);
            long montoIntereses = BigDecimal.valueOf(totalInteresesRepartir)
                    .multiply(porcentaje)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            LiquidacionxPersona detalle = new LiquidacionxPersona();
            detalle.setIdPersona(entry.getKey());
            detalle.setIdLiquidacion(liquidacion.getId());
            detalle.setMontoAhorrado(montoAhorrado);
            detalle.setMontoIntereses(montoIntereses);
            detalle.setPorcentaje(porcentaje.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            detalle.setMontoTotal(montoAhorrado + montoIntereses);
            detalle.setIdEstado(estadoPendiente.getId());
            liquidacionxPersonaRepository.save(detalle);
        }

        return resumenLiquidacion(liquidacion.getId());
    }

    // ---------- Consultar el resumen (totales + detalle por persona) ----------
    public LiquidacionResumenResponse resumenLiquidacion(Long idLiquidacion) {
        Liquidacion liquidacion = liquidacionRepository.findById(idLiquidacion)
                .orElseThrow(() -> new IllegalArgumentException("No existe la liquidación con id " + idLiquidacion));
        List<LiquidacionxPersona> detalle = liquidacionxPersonaRepository.findByIdLiquidacion(idLiquidacion);

        LiquidacionResumenResponse resp = new LiquidacionResumenResponse();
        resp.setLiquidacion(liquidacion);
        resp.setDetalle(detalle);
        return resp;
    }

    // ---------- Pagar a una persona lo que le corresponde ----------
    @Transactional
    public LiquidacionxPersona pagar(Long idLiquidacionxPersona, PagarLiquidacionRequest request) {
        LiquidacionxPersona detalle = liquidacionxPersonaRepository.findById(idLiquidacionxPersona)
                .orElseThrow(() -> new IllegalArgumentException("No existe el registro " + idLiquidacionxPersona));

        Estado estadoPagada = estadoPorNombre(ESTADO_LIQUIDACIONXPERSONA_PAGADA);
        if (detalle.getIdEstado().equals(estadoPagada.getId())) {
            throw new IllegalStateException("Esta parte de la liquidación ya fue pagada");
        }

        Liquidacion liquidacion = liquidacionRepository.findById(detalle.getIdLiquidacion())
                .orElseThrow(() -> new IllegalStateException("Liquidación no encontrada"));

        TipoMovimiento tipoPago = tipoPorNombre(TIPO_PAGO_LIQUIDACION);
        Movimiento pago = new Movimiento();
        pago.setIdNatillera(liquidacion.getIdNatillera());
        pago.setIdPersona(detalle.getIdPersona());
        pago.setIdTipoMovimiento(tipoPago.getId());
        pago.setMonto(detalle.getMontoTotal());
        pago.setFecha(request.getFecha());
        pago.setDescripcion("Pago liquidación #" + liquidacion.getId());
        pago = movimientoRepository.save(pago);

        detalle.setIdMovimientoPago(pago.getId());
        detalle.setIdEstado(estadoPagada.getId());
        return liquidacionxPersonaRepository.save(detalle);
    }
}
