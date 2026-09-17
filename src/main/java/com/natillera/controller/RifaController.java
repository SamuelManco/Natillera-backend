package com.natillera.controller;

import com.natillera.model.Rifa;
import com.natillera.repository.RifaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rifa")
public class RifaController {

    @Autowired
    private RifaRepository repository;

    // Create
    @PostMapping
    public ResponseEntity<Rifa> crear(@RequestBody Rifa entidad) {
        Rifa guardado = repository.save(entidad);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    // Read - todos
    @GetMapping
    public List<Rifa> listarTodos() {
        return repository.findAll();
    }

    // Read - por id
    @GetMapping("/{id}")
    public ResponseEntity<Rifa> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<Rifa> actualizar(@PathVariable Long id, @RequestBody Rifa datos) {
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
