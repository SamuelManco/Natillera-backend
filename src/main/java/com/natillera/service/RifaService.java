package com.natillera.service;

import com.natillera.dto.EstadoRifaResponse;
import com.natillera.dto.ResultadoRifaResponse;
import com.natillera.dto.SortearRifaRequest;
import com.natillera.dto.VenderBoletaRequest;
import com.natillera.model.Boleta;
import com.natillera.model.Estado;
import com.natillera.model.Movimiento;
import com.natillera.model.Natillera;
import com.natillera.model.Rifa;
import com.natillera.model.TipoMovimiento;
import com.natillera.repository.BoletaRepository;
import com.natillera.repository.EstadoRepository;
import com.natillera.repository.MovimientoRepository;
import com.natillera.repository.NatilleraRepository;
import com.natillera.repository.PersonaRepository;
import com.natillera.repository.RifaRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class RifaService {

    // Nombres exactos que deben existir en Tbl_Estado (el DataSeeder los crea)
    private static final String ESTADO_RIFA_FINALIZADA = "Finalizada";
    private static final String ESTADO_BOLETA_VENDIDA = "Vendida";

    // Nombres exactos que deben existir en Tbl_Tipo_Movimiento (el DataSeeder los crea)
    private static final String TIPO_PREMIO_RIFA = "Premio Rifa";
    private static final String TIPO_INGRESO_RIFA = "Ingreso Rifa";

    @Autowired
    private RifaRepository rifaRepository;

    @Autowired
    private BoletaRepository boletaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private NatilleraRepository natilleraRepository;

    @Autowired
    private PersonaRepository personaRepository;

    private Estado estadoPorNombre(String nombre) {
        return estadoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el Estado '" + nombre + "'. Créalo primero en /api/estado"));
    }

    private TipoMovimiento tipoPorNombre(String nombre) {
        return tipoMovimientoRepository.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el Tipo_Movimiento '" + nombre + "'. Créalo primero en /api/tipo-movimiento"));
    }

    private static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Bogota"));
    }

    // El número de la rifa son las 3 últimas cifras de la Lotería de Medellín
    private static String validarNumero(String numero, String queEs) {
        String limpio = numero == null ? "" : numero.trim();
        if (!limpio.matches("\\d{3}")) {
            throw new IllegalArgumentException(queEs + " debe tener exactamente 3 cifras (por ejemplo 007)");
        }
        return limpio;
    }

    // ---------- Vender una boleta ----------
    // IMPORTANTE: esto NUNCA crea un Movimiento. Vender una boleta solo aparta un número.
    // El dinero solo entra a la bitácora cuando se sortea la rifa (ver sortearRifa).
    @Transactional
    public Boleta venderBoleta(Long idRifa, VenderBoletaRequest request) {
        Rifa rifa = rifaRepository.findById(idRifa)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa con id " + idRifa));

        Estado estadoFinalizada = estadoPorNombre(ESTADO_RIFA_FINALIZADA);
        if (rifa.getIdEstado().equals(estadoFinalizada.getId())) {
            throw new IllegalStateException("La rifa " + idRifa + " ya fue sorteada, no se pueden vender más boletas");
        }

        String numero = validarNumero(request.getNumero(), "El número de la boleta");

        if (request.getIdPersona() == null || !personaRepository.existsById(request.getIdPersona())) {
            throw new IllegalArgumentException("El socio que compra la boleta no existe");
        }

        if (boletaRepository.findByIdRifaAndNumero(idRifa, numero).isPresent()) {
            throw new IllegalStateException("El número " + numero + " ya fue vendido en esta rifa");
        }

        long vendidas = boletaRepository.findByIdRifa(idRifa).size();
        if (vendidas >= rifa.getCantidadBoletas()) {
            throw new IllegalStateException("Ya se vendieron todas las boletas de esta rifa");
        }

        Estado estadoVendida = estadoPorNombre(ESTADO_BOLETA_VENDIDA);
        Boleta boleta = new Boleta();
        boleta.setIdRifa(idRifa);
        boleta.setNumero(numero);
        boleta.setIdPersona(request.getIdPersona());
        boleta.setFechaCompra(request.getFecha() != null ? request.getFecha() : hoy());
        boleta.setIdEstado(estadoVendida.getId());
        return boletaRepository.save(boleta);
    }

    // ---------- Sortear la rifa: aquí se genera el ÚNICO movimiento de dinero de toda la rifa ----------
    //  - Si alguien tenía el número: Premio Rifa. Todo lo recaudado se suma al AHORRO del ganador.
    //  - Si nadie lo tenía: Ingreso Rifa. Todo lo recaudado queda en el capital de la natillera
    //    (fondo de intereses, que se reparte entre los socios al liquidar).
    @Transactional
    public ResultadoRifaResponse sortearRifa(Long idRifa, SortearRifaRequest request) {
        Rifa rifa = rifaRepository.findById(idRifa)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa con id " + idRifa));

        Estado estadoFinalizada = estadoPorNombre(ESTADO_RIFA_FINALIZADA);
        if (rifa.getIdEstado().equals(estadoFinalizada.getId())) {
            throw new IllegalStateException("La rifa " + idRifa + " ya fue sorteada");
        }

        String numeroGanador = validarNumero(request.getNumeroGanador(), "El número ganador");
        LocalDate fechaSorteo = request.getFecha() != null ? request.getFecha() : hoy();

        List<Boleta> boletas = boletaRepository.findByIdRifa(idRifa);
        long totalRecaudado = (long) boletas.size() * rifa.getPrecioBoleta();

        // El número ganador SIEMPRE se guarda, sin importar si alguien lo compró o no
        // (es el resultado real de la Lotería de Medellín, un hecho externo)
        rifa.setNumeroGanador(numeroGanador);
        rifa.setIdEstado(estadoFinalizada.getId());
        rifa.setFechaFin(fechaSorteo);
        rifaRepository.save(rifa);

        Optional<Boleta> boletaGanadora = boletaRepository.findByIdRifaAndNumero(idRifa, numeroGanador);

        ResultadoRifaResponse resp = new ResultadoRifaResponse();
        resp.setIdRifa(rifa.getId());
        resp.setNumeroGanador(rifa.getNumeroGanador());
        resp.setTotalRecaudado(totalRecaudado);

        if (boletaGanadora.isPresent()) {
            Boleta ganadora = boletaGanadora.get();
            resp.setIdBoletaGanadora(ganadora.getId());
            resp.setIdPersonaGanadora(ganadora.getIdPersona());

            if (totalRecaudado > 0) {
                TipoMovimiento tipoPremio = tipoPorNombre(TIPO_PREMIO_RIFA);
                Movimiento premio = new Movimiento();
                premio.setIdNatillera(rifa.getIdNatillera());
                premio.setIdPersona(ganadora.getIdPersona());
                premio.setIdTipoMovimiento(tipoPremio.getId());
                premio.setIdRifa(idRifa);
                premio.setMonto(totalRecaudado);
                premio.setFecha(fechaSorteo);
                premio.setDescripcion("Premio de la rifa '" + rifa.getNombre() + "' (número " + numeroGanador
                        + "): se suma al ahorro del ganador");
                movimientoRepository.save(premio);
            }
        } else {
            resp.setIdBoletaGanadora(null);
            resp.setIdPersonaGanadora(null);

            if (totalRecaudado > 0) {
                TipoMovimiento tipoIngreso = tipoPorNombre(TIPO_INGRESO_RIFA);
                Movimiento ingreso = new Movimiento();
                ingreso.setIdNatillera(rifa.getIdNatillera());
                ingreso.setIdPersona(null); // dinero del fondo común, no de un socio
                ingreso.setIdTipoMovimiento(tipoIngreso.getId());
                ingreso.setIdRifa(idRifa);
                ingreso.setMonto(totalRecaudado);
                ingreso.setFecha(fechaSorteo);
                ingreso.setDescripcion("Nadie ganó la rifa '" + rifa.getNombre() + "': el monto queda en el capital de la natillera");
                movimientoRepository.save(ingreso);

                Natillera natillera = natilleraRepository.findById(rifa.getIdNatillera())
                        .orElseThrow(() -> new IllegalStateException("Natillera no encontrada"));
                long totalIngresoActual = natillera.getTotalIngresoRifa() != null ? natillera.getTotalIngresoRifa() : 0L;
                long totalInteresesActual = natillera.getTotalIntereses() != null ? natillera.getTotalIntereses() : 0L;
                natillera.setTotalIngresoRifa(totalIngresoActual + totalRecaudado);
                natillera.setTotalIntereses(totalInteresesActual + totalRecaudado);
                natilleraRepository.save(natillera);
            }
        }

        return resp;
    }

    // ---------- Consultar el estado de una rifa ----------
    public EstadoRifaResponse estadoRifa(Long idRifa) {
        Rifa rifa = rifaRepository.findById(idRifa)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa con id " + idRifa));

        List<Boleta> boletas = boletaRepository.findByIdRifa(idRifa);
        Estado estadoFinalizada = estadoPorNombre(ESTADO_RIFA_FINALIZADA);

        EstadoRifaResponse resp = new EstadoRifaResponse();
        resp.setIdRifa(idRifa);
        resp.setBoletasVendidas(boletas.size());
        resp.setBoletasDisponibles(Math.max(0, rifa.getCantidadBoletas() - boletas.size()));
        resp.setTotalRecaudado((long) boletas.size() * rifa.getPrecioBoleta());
        resp.setFinalizada(rifa.getIdEstado().equals(estadoFinalizada.getId()));
        resp.setNumeroGanador(rifa.getNumeroGanador());
        return resp;
    }

    // ---------- Listar las boletas vendidas de una rifa ----------
    public List<Boleta> boletasDeRifa(Long idRifa) {
        return boletaRepository.findByIdRifa(idRifa);
    }
}
