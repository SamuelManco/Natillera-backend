package com.natillera.repository;

import com.natillera.model.Boleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {

    // Todas las boletas vendidas de una rifa
    List<Boleta> findByIdRifa(Long idRifa);

    // Buscar una boleta específica por número dentro de una rifa (para validar duplicados y encontrar al ganador)
    Optional<Boleta> findByIdRifaAndNumero(Long idRifa, String numero);
}
