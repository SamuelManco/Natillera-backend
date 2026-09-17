package com.natillera.controller;

import com.natillera.dto.CerrarLiquidacionRequest;
import com.natillera.dto.LiquidacionResumenResponse;
import com.natillera.dto.PagarLiquidacionRequest;
import com.natillera.model.LiquidacionxPersona;
import com.natillera.service.LiquidacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/liquidaciones")
public class LiquidacionOperacionesController {

    @Autowired
    private LiquidacionService liquidacionService;

    // Calcular y cerrar la liquidación de un periodo para una natillera
    // (reparte los intereses acumulados proporcionalmente a lo ahorrado por cada persona)
    @PostMapping("/natillera/{idNatillera}/cerrar")
    public ResponseEntity<LiquidacionResumenResponse> cerrar(@PathVariable Long idNatillera, @RequestBody CerrarLiquidacionRequest request) {
        return ResponseEntity.ok(liquidacionService.cerrarLiquidacion(idNatillera, request));
    }

    // Ver el resumen (totales + detalle por persona) de una liquidación ya calculada
    @GetMapping("/{id}/resumen")
    public ResponseEntity<LiquidacionResumenResponse> resumen(@PathVariable Long id) {
        return ResponseEntity.ok(liquidacionService.resumenLiquidacion(id));
    }

    // Pagar la parte que le corresponde a una persona (crea el movimiento de egreso)
    @PostMapping("/detalle/{idLiquidacionxPersona}/pagar")
    public ResponseEntity<LiquidacionxPersona> pagar(@PathVariable Long idLiquidacionxPersona, @RequestBody PagarLiquidacionRequest request) {
        return ResponseEntity.ok(liquidacionService.pagar(idLiquidacionxPersona, request));
    }
}
