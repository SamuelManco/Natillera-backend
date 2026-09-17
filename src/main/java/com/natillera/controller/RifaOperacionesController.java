package com.natillera.controller;

import com.natillera.dto.EstadoRifaResponse;
import com.natillera.dto.ResultadoRifaResponse;
import com.natillera.dto.SortearRifaRequest;
import com.natillera.dto.VenderBoletaRequest;
import com.natillera.model.Boleta;
import com.natillera.service.RifaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rifas")
public class RifaOperacionesController {

    @Autowired
    private RifaService rifaService;

    // Vender una boleta de una rifa
    @PostMapping("/{id}/vender-boleta")
    public ResponseEntity<Boleta> venderBoleta(@PathVariable Long id, @RequestBody VenderBoletaRequest request) {
        Boleta boleta = rifaService.venderBoleta(id, request);
        return new ResponseEntity<>(boleta, HttpStatus.CREATED);
    }

    // Sortear la rifa (definir el número ganador y buscar al dueño de la boleta)
    @PostMapping("/{id}/sortear")
    public ResponseEntity<ResultadoRifaResponse> sortear(@PathVariable Long id, @RequestBody SortearRifaRequest request) {
        return ResponseEntity.ok(rifaService.sortearRifa(id, request));
    }

    // Ver el estado de una rifa (boletas vendidas, disponibles, recaudo, si ya se sorteó)
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoRifaResponse> estado(@PathVariable Long id) {
        return ResponseEntity.ok(rifaService.estadoRifa(id));
    }

    // Listar todas las boletas vendidas de una rifa
    @GetMapping("/{id}/boletas")
    public List<Boleta> boletas(@PathVariable Long id) {
        return rifaService.boletasDeRifa(id);
    }
}
