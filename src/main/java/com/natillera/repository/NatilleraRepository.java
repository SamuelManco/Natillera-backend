package com.natillera.repository;

import com.natillera.model.Natillera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NatilleraRepository extends JpaRepository<Natillera, Long> {
}
