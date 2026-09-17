package com.natillera.repository;

import com.natillera.model.Rifa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RifaRepository extends JpaRepository<Rifa, Long> {
}
