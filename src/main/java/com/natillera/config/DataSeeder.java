package com.natillera.config;

import com.natillera.model.Categoria;
import com.natillera.model.Estado;
import com.natillera.model.Natillera;
import com.natillera.model.Rol;
import com.natillera.model.TipoDocumento;
import com.natillera.model.TipoMovimiento;
import com.natillera.repository.CategoriaRepository;
import com.natillera.repository.EstadoRepository;
import com.natillera.repository.NatilleraRepository;
import com.natillera.repository.RolRepository;
import com.natillera.repository.TipoDocumentoRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;

/**
 * Crea al arrancar los catálogos que la aplicación necesita (roles, tipos de documento,
 * categorías, estados, tipos de movimiento) y una natillera inicial.
 *
 * Es idempotente: solo crea lo que falta. Compara los nombres sin tildes ni mayúsculas,
 * igual que lo hace MySQL, así que nunca duplica registros que ya existan.
 *
 * Los nombres de Estado y Tipo_Movimiento deben coincidir con los que buscan los servicios
 * (PrestamoService, RifaService, LiquidacionService).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private TipoDocumentoRepository tipoDocumentoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EstadoRepository estadoRepository;

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private NatilleraRepository natilleraRepository;

    @Override
    public void run(String... args) {
        sembrarRoles();
        sembrarTiposDocumento();
        sembrarTiposMovimiento();
        sembrarEstadosYNatillera();
        log.info("DataSeeder: catalogos verificados");
    }

    // ---------- Roles ----------
    private void sembrarRoles() {
        rol("admin", "Administrador de la natillera");
        rol("cliente", "Miembro que ahorra");
    }

    private void rol(String nombre, String descripcion) {
        for (Rol r : rolRepository.findAll()) {
            if (igual(r.getNombre(), nombre)) return;
        }
        Rol r = new Rol();
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        rolRepository.save(r);
        log.info("DataSeeder: creado Rol '{}'", nombre);
    }

    // ---------- Tipos de documento ----------
    private void sembrarTiposDocumento() {
        tipoDocumento("Cédula de ciudadanía", "Documento de identidad para mayores de edad");
        tipoDocumento("Tarjeta de identidad", "Documento de identidad para menores de edad");
        tipoDocumento("Cédula de extranjería", "Documento para extranjeros residentes");
        tipoDocumento("Pasaporte", "Documento de viaje");
    }

    private void tipoDocumento(String nombre, String descripcion) {
        for (TipoDocumento t : tipoDocumentoRepository.findAll()) {
            if (igual(t.getNombre(), nombre)) return;
        }
        TipoDocumento t = new TipoDocumento();
        t.setNombre(nombre);
        t.setDescripcion(descripcion);
        tipoDocumentoRepository.save(t);
        log.info("DataSeeder: creado Tipo_Documento '{}'", nombre);
    }

    // ---------- Tipos de movimiento ----------
    private void sembrarTiposMovimiento() {
        tipoMovimiento("Aporte", "Ahorro mensual de un integrante");
        tipoMovimiento("Préstamo Otorgado", "Dinero prestado a un miembro de la natillera");
        tipoMovimiento("Abono a Capital", "Pago que reduce la deuda de un préstamo");
        tipoMovimiento("Interés Préstamo", "Interés generado por un préstamo, va al fondo");
        tipoMovimiento("Ingreso Rifa", "Dinero de una rifa sin ganador, va al fondo");
        tipoMovimiento("Premio Rifa", "Premio pagado al ganador de una rifa");
        tipoMovimiento("Pago Liquidación", "Pago final de una liquidación a un miembro");
    }

    private void tipoMovimiento(String nombre, String descripcion) {
        for (TipoMovimiento t : tipoMovimientoRepository.findAll()) {
            if (igual(t.getNombre(), nombre)) return;
        }
        TipoMovimiento t = new TipoMovimiento();
        t.setNombre(nombre);
        t.setDescripcion(descripcion);
        tipoMovimientoRepository.save(t);
        log.info("DataSeeder: creado Tipo_Movimiento '{}'", nombre);
    }

    // ---------- Categorías, estados y natillera inicial ----------
    private void sembrarEstadosYNatillera() {
        Long catNatillera = categoria("Natillera", "Estados de la natillera");
        Long catRifa = categoria("Rifa", "Estados de una rifa");
        Long catBoleta = categoria("Boleta", "Estados de una boleta");
        Long catLiquidacion = categoria("Liquidacion", "Estados del cierre de una liquidación");
        Long catPago = categoria("Pago de liquidacion", "Estados del pago a cada miembro");

        // Los nombres de estado deben ser únicos entre categorías:
        // los servicios los buscan solo por nombre.
        Long idActiva = estado("Activa", "Natillera en funcionamiento", catNatillera);
        estado("Inactiva", "Natillera cerrada", catNatillera);

        estado("Abierta", "Rifa con boletas a la venta", catRifa);
        estado("Finalizada", "Rifa ya sorteada", catRifa);

        estado("Vendida", "Boleta comprada por un miembro", catBoleta);

        estado("Cerrada", "Liquidación calculada y cerrada", catLiquidacion);

        estado("Pendiente", "Pago pendiente al miembro", catPago);
        estado("Pagada", "Pago ya entregado al miembro", catPago);

        // La app asume que existe al menos una natillera (el front toma la primera)
        if (natilleraRepository.count() == 0) {
            Natillera n = new Natillera();
            n.setNombre("Natillera");
            n.setFechaInicio(LocalDate.now());
            n.setIdEstado(idActiva);
            n.setTotalIngresoRifa(0L);
            n.setTotalInteresesPrestamos(0L);
            n.setTotalIntereses(0L);
            natilleraRepository.save(n);
            log.info("DataSeeder: creada la natillera inicial");
        }
    }

    private Long categoria(String nombre, String descripcion) {
        for (Categoria c : categoriaRepository.findAll()) {
            if (igual(c.getNombre(), nombre)) return c.getId();
        }
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        Categoria guardada = categoriaRepository.save(c);
        log.info("DataSeeder: creada Categoria '{}'", nombre);
        return guardada.getId();
    }

    private Long estado(String nombre, String descripcion, Long idCategoria) {
        for (Estado e : estadoRepository.findAll()) {
            if (igual(e.getNombre(), nombre)) return e.getId();
        }
        Estado e = new Estado();
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        e.setIdCategoria(idCategoria);
        Estado guardado = estadoRepository.save(e);
        log.info("DataSeeder: creado Estado '{}'", nombre);
        return guardado.getId();
    }

    // ---------- Comparación sin tildes ni mayúsculas ----------
    private static boolean igual(String a, String b) {
        return normalizar(a).equals(normalizar(b));
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.trim().toLowerCase();
    }
}