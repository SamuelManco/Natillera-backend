package com.natillera.service;

import com.natillera.dto.AbonarPrestamoRequest;
import com.natillera.dto.EstadoPrestamoResponse;
import com.natillera.dto.OtorgarPrestamoRequest;
import com.natillera.model.Movimiento;
import com.natillera.model.Natillera;
import com.natillera.model.TipoMovimiento;
import com.natillera.repository.MovimientoRepository;
import com.natillera.repository.NatilleraRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PrestamoService {

    // Nombres exactos que deben existir en Tbl_Tipo_Movimiento (créalos primero vía /api/tipo-movimiento)
    private static final String TIPO_PRESTAMO_OTORGADO = "Préstamo Otorgado";
    private static final String TIPO_ABONO_CAPITAL = "Abono a Capital";
    private static final String TIPO_INTERES_PRESTAMO = "Interés Préstamo";

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private NatilleraRepository natilleraRepository;

    private TipoMovimiento tipoPorNombre(String nombre) {
        return tipoMovimientoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el Tipo_Movimiento '" + nombre + "'. Créalo primero en /api/tipo-movimiento"));
    }

    // ---------- Otorgar un préstamo ----------
    public Movimiento otorgarPrestamo(OtorgarPrestamoRequest request) {
        TipoMovimiento tipo = tipoPorNombre(TIPO_PRESTAMO_OTORGADO);

        Movimiento prestamo = new Movimiento();
        prestamo.setIdNatillera(request.getIdNatillera());
        prestamo.setIdPersona(request.getIdPersona());
        prestamo.setIdTipoMovimiento(tipo.getId());
        prestamo.setIdRifa(null);
        prestamo.setIdMovimientoOrigen(null); // este movimiento ES el origen
        prestamo.setTasaInteres(request.getTasaInteres());
        prestamo.setMonto(request.getMonto());
        prestamo.setMontoActual(request.getMonto()); // al nacer, debe el 100%
        prestamo.setFecha(request.getFecha());
        prestamo.setDescripcion("Préstamo otorgado");

        return movimientoRepository.save(prestamo);
    }

    // ---------- Registrar un abono ----------
    @Transactional
    public EstadoPrestamoResponse abonarPrestamo(Long idPrestamo, AbonarPrestamoRequest request) {
        Movimiento prestamo = movimientoRepository.findById(idPrestamo)
                .orElseThrow(() -> new IllegalArgumentException("No existe el préstamo con id " + idPrestamo));

        TipoMovimiento tipoPrestamo = tipoPorNombre(TIPO_PRESTAMO_OTORGADO);
        if (!prestamo.getIdTipoMovimiento().equals(tipoPrestamo.getId())) {
            throw new IllegalArgumentException("El movimiento " + idPrestamo + " no es un préstamo otorgado");
        }

        // 1. Determinar desde cuándo se cuentan los meses: desde el último interés cobrado,
        //    o si es el primer abono, desde la fecha del préstamo.
        List<Movimiento> historial = movimientoRepository.findByIdMovimientoOrigenOrderByFechaAsc(idPrestamo);
        TipoMovimiento tipoInteres = tipoPorNombre(TIPO_INTERES_PRESTAMO);
        LocalDate fechaBase = historial.stream()
                .filter(m -> m.getIdTipoMovimiento().equals(tipoInteres.getId()))
                .map(Movimiento::getFecha)
                .max(LocalDate::compareTo)
                .orElse(prestamo.getFecha());

        long meses = Math.max(0, ChronoUnit.MONTHS.between(fechaBase, request.getFecha()));

        // 2. Interés simple: siempre sobre el monto ORIGINAL del préstamo, no sobre el saldo restante
        BigDecimal tasa = prestamo.getTasaInteres() != null ? prestamo.getTasaInteres() : BigDecimal.ZERO;
        BigDecimal interesCalculado = BigDecimal.valueOf(prestamo.getMonto())
                .multiply(tasa.divide(BigDecimal.valueOf(100)))
                .multiply(BigDecimal.valueOf(meses))
                .setScale(0, RoundingMode.HALF_UP);
        long interesLong = interesCalculado.longValue();

        // 3. Crear la fila de Abono a Capital
        TipoMovimiento tipoAbono = tipoPorNombre(TIPO_ABONO_CAPITAL);
        Movimiento abono = new Movimiento();
        abono.setIdNatillera(prestamo.getIdNatillera());
        abono.setIdPersona(prestamo.getIdPersona());
        abono.setIdTipoMovimiento(tipoAbono.getId());
        abono.setIdMovimientoOrigen(idPrestamo);
        abono.setMonto(request.getMontoAbonoCapital());
        abono.setFecha(request.getFecha());
        abono.setDescripcion("Abono a préstamo #" + idPrestamo);
        movimientoRepository.save(abono);

        // 4. Crear la fila de Interés Préstamo (va al fondo común, Id_Persona = NULL)
        if (interesLong > 0) {
            Movimiento interes = new Movimiento();
            interes.setIdNatillera(prestamo.getIdNatillera());
            interes.setIdPersona(null); // el interés es del fondo, no de la persona
            interes.setIdTipoMovimiento(tipoInteres.getId());
            interes.setIdMovimientoOrigen(idPrestamo);
            interes.setMonto(interesLong);
            interes.setFecha(request.getFecha());
            interes.setDescripcion("Interés de " + meses + " mes(es) del préstamo #" + idPrestamo);
            movimientoRepository.save(interes);

            // 5. Acumular el interés en Natillera (histórico, según pidió el profesor)
            Natillera natillera = natilleraRepository.findById(prestamo.getIdNatillera())
                    .orElseThrow(() -> new IllegalStateException("Natillera no encontrada"));
            long totalPrestamosActual = natillera.getTotalInteresesPrestamos() != null ? natillera.getTotalInteresesPrestamos() : 0L;
            long totalInteresesActual = natillera.getTotalIntereses() != null ? natillera.getTotalIntereses() : 0L;
            natillera.setTotalInteresesPrestamos(totalPrestamosActual + interesLong);
            natillera.setTotalIntereses(totalInteresesActual + interesLong);
            natilleraRepository.save(natillera);
        }

        // 6. Reducir el saldo pendiente del préstamo
        long nuevoMontoActual = Math.max(0, prestamo.getMontoActual() - request.getMontoAbonoCapital());
        prestamo.setMontoActual(nuevoMontoActual);
        movimientoRepository.save(prestamo);

        return estadoPrestamo(idPrestamo);
    }

    // ---------- Consultar el estado completo de un préstamo ----------
    public EstadoPrestamoResponse estadoPrestamo(Long idPrestamo) {
        Movimiento prestamo = movimientoRepository.findById(idPrestamo)
                .orElseThrow(() -> new IllegalArgumentException("No existe el préstamo con id " + idPrestamo));

        List<Movimiento> historial = movimientoRepository.findByIdMovimientoOrigenOrderByFechaAsc(idPrestamo);
        TipoMovimiento tipoAbono = tipoPorNombre(TIPO_ABONO_CAPITAL);
        TipoMovimiento tipoInteres = tipoPorNombre(TIPO_INTERES_PRESTAMO);

        long totalAbonado = historial.stream()
                .filter(m -> m.getIdTipoMovimiento().equals(tipoAbono.getId()))
                .mapToLong(Movimiento::getMonto)
                .sum();
        long totalInteres = historial.stream()
                .filter(m -> m.getIdTipoMovimiento().equals(tipoInteres.getId()))
                .mapToLong(Movimiento::getMonto)
                .sum();

        EstadoPrestamoResponse resp = new EstadoPrestamoResponse();
        resp.setIdPrestamo(idPrestamo);
        resp.setMontoOriginal(prestamo.getMonto());
        resp.setMontoActual(prestamo.getMontoActual());
        resp.setTotalAbonadoCapital(totalAbonado);
        resp.setTotalInteresPagado(totalInteres);
        resp.setPagadoCompleto(prestamo.getMontoActual() != null && prestamo.getMontoActual() <= 0);
        resp.setHistorial(historial);
        return resp;
    }

    // ---------- Listar préstamos activos (con saldo pendiente) ----------
    public List<Movimiento> prestamosActivos() {
        TipoMovimiento tipoPrestamo = tipoPorNombre(TIPO_PRESTAMO_OTORGADO);
        return movimientoRepository.findByIdTipoMovimiento(tipoPrestamo.getId())
                .stream()
                .filter(m -> m.getMontoActual() != null && m.getMontoActual() > 0)
                .toList();
    }
}
