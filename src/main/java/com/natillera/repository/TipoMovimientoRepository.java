package com.natillera.repository;

import com.natillera.model.TipoMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoMovimientoRepository extends JpaRepository<TipoMovimiento, Long> {
    Optional<TipoMovimiento> findByNombre(String nombre);
}
