package com.natillera.repository;

import com.natillera.model.LiquidacionxPersona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LiquidacionxPersonaRepository extends JpaRepository<LiquidacionxPersona, Long> {

    // Todo el detalle por persona de una liquidación específica
    List<LiquidacionxPersona> findByIdLiquidacion(Long idLiquidacion);
}
