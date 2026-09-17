package com.natillera.dto;

import com.natillera.model.Liquidacion;
import com.natillera.model.LiquidacionxPersona;
import lombok.Data;

import java.util.List;

@Data
public class LiquidacionResumenResponse {
    private Liquidacion liquidacion;
    private List<LiquidacionxPersona> detalle; // el reparto calculado para cada persona
}
