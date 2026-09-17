package com.natillera.repository;

import com.natillera.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    // Todos los abonos/intereses que apuntan a un préstamo específico
    List<Movimiento> findByIdMovimientoOrigenOrderByFechaAsc(Long idMovimientoOrigen);

    // Todos los movimientos de un tipo específico (ej. todos los "Préstamo Otorgado")
    List<Movimiento> findByIdTipoMovimiento(Long idTipoMovimiento);

    // Movimientos de una persona (ej. sus aportes, o sus préstamos)
    List<Movimiento> findByIdPersonaAndIdTipoMovimiento(Long idPersona, Long idTipoMovimiento);

    // Movimientos ligados a una rifa (ej. el resultado del sorteo)
    List<Movimiento> findByIdRifa(Long idRifa);

    // Movimientos de un tipo, dentro de un rango de fechas (para liquidar SOLO el periodo actual,
    // no el histórico completo — evita repartir dos veces el mismo dinero en periodos distintos)
    List<Movimiento> findByIdNatilleraAndIdTipoMovimientoAndFechaBetween(
            Long idNatillera, Long idTipoMovimiento, LocalDate fechaInicio, LocalDate fechaFin);
}
