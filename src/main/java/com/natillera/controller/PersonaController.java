package com.natillera.controller;

import com.natillera.model.Persona;
import com.natillera.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/persona")
public class PersonaController {

    @Autowired
    private PersonaRepository repository;

    // Create
    @PostMapping
    public ResponseEntity<Persona> crear(@RequestBody Persona entidad) {
        Persona guardado = repository.save(entidad);
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    // Read - todos
    @GetMapping
    public List<Persona> listarTodos() {
        return repository.findAll();
    }

    // Read - por id
    @GetMapping("/{id}")
    public ResponseEntity<Persona> buscarPorId(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<Persona> actualizar(@PathVariable Long id, @RequestBody Persona datos) {
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
