package com.natillera.controller;

import com.natillera.model.Boleta;
import com.natillera.repository.BoletaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/boleta")
public class BoletaController {

    @Autowired
    private BoletaRepository repository;

    // Create
    @PostMapping
    public ResponseEntity<Boleta> crear(@RequestBody Boleta entidad) {
        Boleta guardado = repository.save(entidad);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    // Read - todos
    @GetMapping
    public List<Boleta> listarTodos() {
        return repository.findAll();
    }

    // Read - por id
    @GetMapping("/{id}")
    public ResponseEntity<Boleta> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<Boleta> actualizar(@PathVariable Long id, @RequestBody Boleta datos) {
        return repository.findById(id)
                .map(existente -> {
                    datos.setId(id);
                    return ResponseEntity.ok(repository.save(datos));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
