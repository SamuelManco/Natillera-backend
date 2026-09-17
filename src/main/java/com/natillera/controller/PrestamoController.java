package com.natillera.controller;

import com.natillera.dto.AbonarPrestamoRequest;
import com.natillera.dto.EstadoPrestamoResponse;
import com.natillera.dto.OtorgarPrestamoRequest;
import com.natillera.model.Movimiento;
import com.natillera.service.PrestamoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    @Autowired
    private PrestamoService prestamoService;

    // Otorgar un préstamo nuevo
    @PostMapping("/otorgar")
    public ResponseEntity<Movimiento> otorgar(@RequestBody OtorgarPrestamoRequest request) {
        Movimiento creado = prestamoService.otorgarPrestamo(request);
        return new ResponseEntity<>(creado, HttpStatus.CREATED);
    }

    // Registrar un abono a un préstamo existente
    @PostMapping("/{id}/abonar")
    public ResponseEntity<EstadoPrestamoResponse> abonar(@PathVariable Long id, @RequestBody AbonarPrestamoRequest request) {
        return ResponseEntity.ok(prestamoService.abonarPrestamo(id, request));
    }

    // Ver el estado completo de un préstamo (saldo, historial de abonos e intereses)
    @GetMapping("/{id}/estado")
    public ResponseEntity<EstadoPrestamoResponse> estado(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoService.estadoPrestamo(id));
    }

    // Listar todos los préstamos que aún tienen saldo pendiente
    @GetMapping("/activos")
    public List<Movimiento> activos() {
        return prestamoService.prestamosActivos();
    }
}
