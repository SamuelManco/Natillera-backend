package com.natillera.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SortearRifaRequest {
    private String numeroGanador;
    private LocalDate fecha;
}
