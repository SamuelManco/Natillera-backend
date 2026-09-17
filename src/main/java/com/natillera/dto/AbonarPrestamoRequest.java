package com.natillera.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AbonarPrestamoRequest {
    private Long montoAbonoCapital;
    private LocalDate fecha;
}
