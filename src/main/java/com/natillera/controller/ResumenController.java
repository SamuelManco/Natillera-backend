package com.natillera.controller;

import com.natillera.dto.NatilleraResumenResponse;
import com.natillera.service.NatilleraResumenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/resumen")
public class ResumenController {

    @Autowired
    private NatilleraResumenService resumenService;

    // Totales de la natillera. Los parámetros desde/hasta son opcionales (formato 2026-01-31).
    @GetMapping
    public NatilleraResumenResponse resumen(
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return resumenService.resumen(desde, hasta);
    }
}
