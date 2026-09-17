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
import com.natillera.repository.RifaRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class RifaService {

    // Nombres exactos que deben existir en Tbl_Estado (créalos primero vía /api/estado)
    private static final String ESTADO_RIFA_FINALIZADA = "Finalizada";
    private static final String ESTADO_BOLETA_VENDIDA = "Vendida";

    // Nombres exactos que deben existir en Tbl_Tipo_Movimiento (créalos primero vía /api/tipo-movimiento)
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

        if (boletaRepository.findByIdRifaAndNumero(idRifa, request.getNumero()).isPresent()) {
            throw new IllegalStateException("El número " + request.getNumero() + " ya fue vendido en esta rifa");
        }

        long vendidas = boletaRepository.findByIdRifa(idRifa).size();
        if (vendidas >= rifa.getCantidadBoletas()) {
            throw new IllegalStateException("Ya se vendieron todas las boletas de esta rifa");
        }

        Estado estadoVendida = estadoPorNombre(ESTADO_BOLETA_VENDIDA);
        Boleta boleta = new Boleta();
        boleta.setIdRifa(idRifa);
        boleta.setNumero(request.getNumero());
        boleta.setIdPersona(request.getIdPersona());
        boleta.setFechaCompra(request.getFecha());
        boleta.setIdEstado(estadoVendida.getId());
        return boletaRepository.save(boleta);
    }

    // ---------- Sortear la rifa: aquí sí se genera el ÚNICO movimiento de dinero de toda la rifa ----------
    @Transactional
    public ResultadoRifaResponse sortearRifa(Long idRifa, SortearRifaRequest request) {
        Rifa rifa = rifaRepository.findById(idRifa)
                .orElseThrow(() -> new IllegalArgumentException("No existe la rifa con id " + idRifa));

        Estado estadoFinalizada = estadoPorNombre(ESTADO_RIFA_FINALIZADA);
        if (rifa.getIdEstado().equals(estadoFinalizada.getId())) {
            throw new IllegalStateException("La rifa " + idRifa + " ya fue sorteada");
        }

        List<Boleta> boletas = boletaRepository.findByIdRifa(idRifa);
        long totalRecaudado = (long) boletas.size() * rifa.getPrecioBoleta();

        // El número ganador SIEMPRE se guarda, sin importar si alguien lo compró o no
        // (es el resultado real de la Lotería de Medellín, un hecho externo)
        rifa.setNumeroGanador(request.getNumeroGanador());
        rifa.setIdEstado(estadoFinalizada.getId());
        rifa.setFechaFin(request.getFecha());
        rifaRepository.save(rifa);

        Optional<Boleta> boletaGanadora = boletaRepository.findByIdRifaAndNumero(idRifa, request.getNumeroGanador());

        ResultadoRifaResponse resp = new ResultadoRifaResponse();
        resp.setIdRifa(rifa.getId());
        resp.setNumeroGanador(rifa.getNumeroGanador());
        resp.setTotalRecaudado(totalRecaudado);

        if (boletaGanadora.isPresent()) {
            // Caso 1: alguien tenía el número -> Premio Rifa, el dinero es de esa persona,
            // y NO entra al fondo común (Natillera no se toca aquí)
            Boleta ganadora = boletaGanadora.get();

            TipoMovimiento tipoPremio = tipoPorNombre(TIPO_PREMIO_RIFA);
            Movimiento premio = new Movimiento();
            premio.setIdNatillera(rifa.getIdNatillera());
            premio.setIdPersona(ganadora.getIdPersona());
            premio.setIdTipoMovimiento(tipoPremio.getId());
            premio.setIdRifa(idRifa);
            premio.setMonto(totalRecaudado);
            premio.setFecha(request.getFecha());
            premio.setDescripcion("Premio de la rifa '" + rifa.getNombre() + "' - número " + request.getNumeroGanador());
            movimientoRepository.save(premio);

            resp.setIdBoletaGanadora(ganadora.getId());
            resp.setIdPersonaGanadora(ganadora.getIdPersona());
        } else {
            // Caso 2: nadie tenía el número -> Ingreso Rifa, Id_Persona = NULL,
            // el dinero SÍ entra al fondo común de la natillera
            TipoMovimiento tipoIngreso = tipoPorNombre(TIPO_INGRESO_RIFA);
            Movimiento ingreso = new Movimiento();
            ingreso.setIdNatillera(rifa.getIdNatillera());
            ingreso.setIdPersona(null);
            ingreso.setIdTipoMovimiento(tipoIngreso.getId());
            ingreso.setIdRifa(idRifa);
            ingreso.setMonto(totalRecaudado);
            ingreso.setFecha(request.getFecha());
            ingreso.setDescripcion("Nadie ganó la rifa '" + rifa.getNombre() + "', queda en el fondo");
            movimientoRepository.save(ingreso);

            Natillera natillera = natilleraRepository.findById(rifa.getIdNatillera())
                    .orElseThrow(() -> new IllegalStateException("Natillera no encontrada"));
            long totalIngresoActual = natillera.getTotalIngresoRifa() != null ? natillera.getTotalIngresoRifa() : 0L;
            long totalInteresesActual = natillera.getTotalIntereses() != null ? natillera.getTotalIntereses() : 0L;
            natillera.setTotalIngresoRifa(totalIngresoActual + totalRecaudado);
            natillera.setTotalIntereses(totalInteresesActual + totalRecaudado);
            natilleraRepository.save(natillera);

            resp.setIdBoletaGanadora(null);
            resp.setIdPersonaGanadora(null);
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
