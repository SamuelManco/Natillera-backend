package com.natillera.service;

import com.natillera.dto.AbonarPrestamoRequest;
import com.natillera.dto.AlertaPrestamoResponse;
import com.natillera.dto.EstadoPrestamoResponse;
import com.natillera.dto.OtorgarPrestamoRequest;
import com.natillera.model.Movimiento;
import com.natillera.model.Natillera;
import com.natillera.model.TipoMovimiento;
import com.natillera.repository.MovimientoRepository;
import com.natillera.repository.NatilleraRepository;
import com.natillera.repository.PersonaRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class PrestamoService {

    // Nombres exactos que deben existir en Tbl_Tipo_Movimiento (el DataSeeder los crea)
    private static final String TIPO_PRESTAMO_OTORGADO = "Préstamo Otorgado";
    private static final String TIPO_ABONO_CAPITAL = "Abono a Capital";
    private static final String TIPO_INTERES_PRESTAMO = "Interés Préstamo";

    // Mes (1-12) en el que vence el plazo para abonar. La natillera termina en diciembre,
    // así que los abonos se aceptan hasta el último día de noviembre.
    @Value("${natillera.abonos.mes-limite:11}")
    private int mesLimiteAbonos;

    // Con cuántos días de anticipación empieza a salir la alerta
    @Value("${natillera.alertas.dias-previos:30}")
    private int diasPreviosAlerta;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private NatilleraRepository natilleraRepository;

    @Autowired
    private PersonaRepository personaRepository;

    private TipoMovimiento tipoPorNombre(String nombre) {
        return tipoMovimientoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el Tipo_Movimiento '" + nombre + "'. Créalo primero en /api/tipo-movimiento"));
    }

    // Último día en que se aceptan abonos para los préstamos de ese año
    private LocalDate fechaLimiteAbonos(int anio) {
        return YearMonth.of(anio, mesLimiteAbonos).atEndOfMonth();
    }

    private static String fecha(LocalDate f) {
        return f.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String pesos(long valor) {
        return "$" + NumberFormat.getIntegerInstance(new Locale("es", "CO")).format(valor);
    }

    // ---------- Otorgar un préstamo ----------
    public Movimiento otorgarPrestamo(OtorgarPrestamoRequest request) {
        if (request.getIdNatillera() == null || request.getIdPersona() == null) {
            throw new IllegalArgumentException("Faltan la natillera o el socio del préstamo");
        }
        if (request.getMonto() == null || request.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del préstamo debe ser mayor a cero");
        }
        if (request.getTasaInteres() == null || request.getTasaInteres().signum() < 0) {
            throw new IllegalArgumentException("La tasa de interés es obligatoria y no puede ser negativa");
        }
        if (request.getFecha() == null) {
            throw new IllegalArgumentException("Falta la fecha del préstamo");
        }
        if (!personaRepository.existsById(request.getIdPersona())) {
            throw new IllegalArgumentException("El socio no existe");
        }

        // No se presta si ya no hay tiempo de abonar
        LocalDate limite = fechaLimiteAbonos(request.getFecha().getYear());
        if (request.getFecha().isAfter(limite)) {
            throw new IllegalArgumentException("No se pueden otorgar préstamos después del " + fecha(limite)
                    + ", porque ya no habría tiempo para abonar");
        }

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

        if (request.getMontoAbonoCapital() == null || request.getMontoAbonoCapital() <= 0) {
            throw new IllegalArgumentException("El abono debe ser mayor a cero");
        }
        if (request.getFecha() == null) {
            throw new IllegalArgumentException("Falta la fecha del abono");
        }

        // 1. Plazo: la natillera termina en diciembre, solo se abona hasta noviembre
        LocalDate limite = fechaLimiteAbonos(prestamo.getFecha().getYear());
        if (request.getFecha().isAfter(limite)) {
            throw new IllegalArgumentException("Ya no se aceptan abonos: el plazo venció el " + fecha(limite));
        }
        if (request.getFecha().isBefore(prestamo.getFecha())) {
            throw new IllegalArgumentException("La fecha del abono no puede ser anterior a la del préstamo ("
                    + fecha(prestamo.getFecha()) + ")");
        }

        // 2. El abono no puede superar lo que se debe
        long saldo = prestamo.getMontoActual() != null ? prestamo.getMontoActual() : prestamo.getMonto();
        if (saldo <= 0) {
            throw new IllegalStateException("Este préstamo ya está pagado por completo");
        }
        if (request.getMontoAbonoCapital() > saldo) {
            throw new IllegalArgumentException("El abono (" + pesos(request.getMontoAbonoCapital())
                    + ") supera el saldo pendiente (" + pesos(saldo) + ")");
        }

        // 3. Desde cuándo se cuentan los meses: desde el último interés cobrado,
        //    o si es el primer abono, desde la fecha del préstamo.
        List<Movimiento> historial = movimientoRepository.findByIdMovimientoOrigenOrderByFechaAsc(idPrestamo);
        TipoMovimiento tipoInteres = tipoPorNombre(TIPO_INTERES_PRESTAMO);
        LocalDate fechaBase = historial.stream()
                .filter(m -> m.getIdTipoMovimiento().equals(tipoInteres.getId()))
                .map(Movimiento::getFecha)
                .max(LocalDate::compareTo)
                .orElse(prestamo.getFecha());

        long meses = Math.max(0, ChronoUnit.MONTHS.between(fechaBase, request.getFecha()));

        // 4. Interés simple: siempre sobre el monto ORIGINAL del préstamo, no sobre el saldo restante
        BigDecimal tasa = prestamo.getTasaInteres() != null ? prestamo.getTasaInteres() : BigDecimal.ZERO;
        BigDecimal interesCalculado = BigDecimal.valueOf(prestamo.getMonto())
                .multiply(tasa.divide(BigDecimal.valueOf(100)))
                .multiply(BigDecimal.valueOf(meses))
                .setScale(0, RoundingMode.HALF_UP);
        long interesLong = interesCalculado.longValue();

        // 5. Fila de Abono a Capital
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

        // 6. Fila de Interés Préstamo (va al fondo común, Id_Persona = NULL)
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

            // Acumular el interés en Natillera (histórico)
            Natillera natillera = natilleraRepository.findById(prestamo.getIdNatillera())
                    .orElseThrow(() -> new IllegalStateException("Natillera no encontrada"));
            long totalPrestamosActual = natillera.getTotalInteresesPrestamos() != null ? natillera.getTotalInteresesPrestamos() : 0L;
            long totalInteresesActual = natillera.getTotalIntereses() != null ? natillera.getTotalIntereses() : 0L;
            natillera.setTotalInteresesPrestamos(totalPrestamosActual + interesLong);
            natillera.setTotalIntereses(totalInteresesActual + interesLong);
            natilleraRepository.save(natillera);
        }

        // 7. Reducir el saldo pendiente del préstamo
        prestamo.setMontoActual(saldo - request.getMontoAbonoCapital());
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
        resp.setFechaLimiteAbonos(fechaLimiteAbonos(prestamo.getFecha().getYear()));
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

    // ---------- Alertas: préstamos con saldo cuando se acerca (o ya pasó) el último día de abonos ----------
    // El parámetro "hoy" es opcional: sirve para probar la alerta con otra fecha.
    public List<AlertaPrestamoResponse> alertas(LocalDate hoy) {
        LocalDate fechaActual = hoy != null ? hoy : LocalDate.now(ZoneId.of("America/Bogota"));
        List<AlertaPrestamoResponse> alertas = new ArrayList<>();

        for (Movimiento prestamo : prestamosActivos()) {
            LocalDate limite = fechaLimiteAbonos(prestamo.getFecha().getYear());
            long dias = ChronoUnit.DAYS.between(fechaActual, limite);
            if (dias > diasPreviosAlerta) {
                continue; // todavía falta mucho, no hay alerta
            }

            String nombre = "Socio #" + prestamo.getIdPersona();
            if (prestamo.getIdPersona() != null) {
                nombre = personaRepository.findById(prestamo.getIdPersona())
                        .map(p -> p.getNombre() + " " + p.getApellido())
                        .orElse(nombre);
            }

            long saldo = prestamo.getMontoActual();
            AlertaPrestamoResponse alerta = new AlertaPrestamoResponse();
            alerta.setIdPrestamo(prestamo.getId());
            alerta.setIdPersona(prestamo.getIdPersona());
            alerta.setNombrePersona(nombre);
            alerta.setSaldoPendiente(saldo);
            alerta.setFechaLimite(limite);
            alerta.setDiasRestantes(dias);

            if (dias < 0) {
                alerta.setNivel("VENCIDO");
                alerta.setMensaje(nombre + " aún debe " + pesos(saldo) + " del préstamo #" + prestamo.getId()
                        + ". El plazo para abonar venció el " + fecha(limite) + ".");
            } else if (dias == 0) {
                alerta.setNivel("PROXIMO");
                alerta.setMensaje(nombre + " aún debe " + pesos(saldo) + " del préstamo #" + prestamo.getId()
                        + ". Hoy es el último día para abonar (" + fecha(limite) + ").");
            } else {
                alerta.setNivel("PROXIMO");
                alerta.setMensaje(nombre + " aún debe " + pesos(saldo) + " del préstamo #" + prestamo.getId()
                        + ". Faltan " + dias + " día(s) para el último día de abonos (" + fecha(limite) + ").");
            }
            alertas.add(alerta);
        }

        alertas.sort(Comparator.comparingLong(AlertaPrestamoResponse::getDiasRestantes));
        return alertas;
    }
}
