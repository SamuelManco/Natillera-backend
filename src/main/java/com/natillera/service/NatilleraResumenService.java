package com.natillera.service;

import com.natillera.dto.NatilleraResumenResponse;
import com.natillera.model.Liquidacion;
import com.natillera.model.Movimiento;
import com.natillera.model.Natillera;
import com.natillera.model.Persona;
import com.natillera.model.TipoMovimiento;
import com.natillera.repository.LiquidacionRepository;
import com.natillera.repository.MovimientoRepository;
import com.natillera.repository.NatilleraRepository;
import com.natillera.repository.PersonaRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NatilleraResumenService {

    private static final String TIPO_APORTE = "Aporte";
    private static final String TIPO_PREMIO_RIFA = "Premio Rifa";
    private static final String TIPO_INGRESO_RIFA = "Ingreso Rifa";
    private static final String TIPO_INTERES_PRESTAMO = "Interés Préstamo";
    private static final String TIPO_PRESTAMO_OTORGADO = "Préstamo Otorgado";
    private static final String TIPO_PAGO_LIQUIDACION = "Pago Liquidación";

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private NatilleraRepository natilleraRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private LiquidacionRepository liquidacionRepository;

    // Movimientos de un tipo, de esta natillera, dentro del rango opcional de fechas.
    // Si el tipo todavía no existe en el catálogo, devuelve lista vacía (el resumen igual funciona).
    private List<Movimiento> movimientos(String nombreTipo, Long idNatillera, LocalDate desde, LocalDate hasta) {
        Optional<TipoMovimiento> tipo = tipoMovimientoRepository.findByNombre(nombreTipo);
        if (tipo.isEmpty()) {
            return new ArrayList<>();
        }
        List<Movimiento> resultado = new ArrayList<>();
        for (Movimiento m : movimientoRepository.findByIdTipoMovimiento(tipo.get().getId())) {
            if (!idNatillera.equals(m.getIdNatillera())) continue;
            if (desde != null && m.getFecha().isBefore(desde)) continue;
            if (hasta != null && m.getFecha().isAfter(hasta)) continue;
            resultado.add(m);
        }
        return resultado;
    }

    private static long suma(List<Movimiento> lista) {
        long total = 0;
        for (Movimiento m : lista) {
            total += m.getMonto();
        }
        return total;
    }

    public NatilleraResumenResponse resumen(LocalDate desde, LocalDate hasta, boolean historialCompleto) {
        Natillera natillera = natilleraRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay ninguna natillera creada"));
        Long idNatillera = natillera.getId();

        // Periodo actual: si no se pide un rango de fechas ni el historial completo, los totales
        // empiezan el día siguiente al último periodo liquidado. Así, al cerrar un periodo,
        // todo aparece en cero para empezar de nuevo (sin borrar el historial).
        LocalDate ultimoCierre = liquidacionRepository.findAll().stream()
                .filter(l -> idNatillera.equals(l.getIdNatillera()) && l.getFechaFin() != null)
                .map(Liquidacion::getFechaFin)
                .max(LocalDate::compareTo)
                .orElse(null);
        boolean periodoActual = false;
        if (desde == null && !historialCompleto && ultimoCierre != null) {
            desde = ultimoCierre.plusDays(1);
            periodoActual = true;
        }

        List<Movimiento> aportes = movimientos(TIPO_APORTE, idNatillera, desde, hasta);
        List<Movimiento> premios = movimientos(TIPO_PREMIO_RIFA, idNatillera, desde, hasta);
        List<Movimiento> ingresosRifa = movimientos(TIPO_INGRESO_RIFA, idNatillera, desde, hasta);
        List<Movimiento> interesesPrestamos = movimientos(TIPO_INTERES_PRESTAMO, idNatillera, desde, hasta);
        List<Movimiento> pagos = movimientos(TIPO_PAGO_LIQUIDACION, idNatillera, desde, hasta);

        long totalAportes = suma(aportes);
        long totalPremios = suma(premios);
        long totalAhorrado = totalAportes + totalPremios;
        long totalInteresesPrestamos = suma(interesesPrestamos);
        long totalIngresoRifa = suma(ingresosRifa);
        long totalIntereses = totalInteresesPrestamos + totalIngresoRifa;
        long totalCapital = totalAhorrado + totalIntereses;
        long totalPagos = suma(pagos);

        // Préstamos por cobrar: saldo actual de los préstamos que todavía deben plata
        long porCobrar = 0;
        int activos = 0;
        for (Movimiento p : movimientos(TIPO_PRESTAMO_OTORGADO, idNatillera, null, null)) {
            long saldo = p.getMontoActual() != null ? p.getMontoActual() : 0L;
            if (saldo > 0) {
                porCobrar += saldo;
                activos++;
            }
        }

        // Ahorro de cada socio (aportes + premios de rifa)
        Map<Long, Long> ahorroPorPersona = new HashMap<>();
        for (Movimiento m : aportes) {
            if (m.getIdPersona() != null) ahorroPorPersona.merge(m.getIdPersona(), m.getMonto(), Long::sum);
        }
        for (Movimiento m : premios) {
            if (m.getIdPersona() != null) ahorroPorPersona.merge(m.getIdPersona(), m.getMonto(), Long::sum);
        }
        List<Persona> personas = personaRepository.findAll();
        List<NatilleraResumenResponse.AhorroSocio> porSocio = new ArrayList<>();
        for (Persona p : personas) {
            NatilleraResumenResponse.AhorroSocio a = new NatilleraResumenResponse.AhorroSocio();
            a.setIdPersona(p.getId());
            a.setNombre(p.getNombre() + " " + p.getApellido());
            a.setAhorrado(ahorroPorPersona.getOrDefault(p.getId(), 0L));
            a.setCuotaMinima(p.getCuotaMinima());
            porSocio.add(a);
        }
        porSocio.sort(Comparator.comparingLong(NatilleraResumenResponse.AhorroSocio::getAhorrado).reversed());

        NatilleraResumenResponse resp = new NatilleraResumenResponse();
        resp.setIdNatillera(idNatillera);
        resp.setNombreNatillera(natillera.getNombre());
        resp.setDesde(desde);
        resp.setHasta(hasta);
        resp.setTotalAportes(totalAportes);
        resp.setTotalPremiosRifa(totalPremios);
        resp.setTotalAhorrado(totalAhorrado);
        resp.setInteresesPrestamos(totalInteresesPrestamos);
        resp.setIngresosRifa(totalIngresoRifa);
        resp.setTotalIntereses(totalIntereses);
        resp.setTotalCapital(totalCapital);
        resp.setPrestamosPorCobrar(porCobrar);
        resp.setPrestamosActivos(activos);
        resp.setPagosLiquidacion(totalPagos);
        // La caja es un saldo de todo el tiempo: no depende del periodo que se esté viendo
        long entradasTotales = suma(movimientos(TIPO_APORTE, idNatillera, null, null))
                + suma(movimientos(TIPO_PREMIO_RIFA, idNatillera, null, null))
                + suma(movimientos(TIPO_INGRESO_RIFA, idNatillera, null, null))
                + suma(movimientos(TIPO_INTERES_PRESTAMO, idNatillera, null, null));
        long pagosTotales = suma(movimientos(TIPO_PAGO_LIQUIDACION, idNatillera, null, null));
        resp.setDisponibleEnCaja(entradasTotales - porCobrar - pagosTotales);
        resp.setPeriodoActual(periodoActual);
        resp.setUltimoCierre(ultimoCierre);
        resp.setNumeroSocios(personas.size());
        resp.setAhorroPorSocio(porSocio);
        return resp;
    }
}
