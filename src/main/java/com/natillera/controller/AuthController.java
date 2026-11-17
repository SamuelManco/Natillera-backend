package com.natillera.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.natillera.model.Persona;
import com.natillera.repository.PersonaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PersonaRepository personaRepository;
    private final String googleClientId;
    private final String adminRoleId;

    public AuthController(
            PersonaRepository personaRepository,
            @Value("${google.client.id}") String googleClientId,
            @Value("${app.admin.role-id:}") String adminRoleId) {
        this.personaRepository = personaRepository;
        this.googleClientId = googleClientId;
        this.adminRoleId = adminRoleId;
    }

    @PostMapping("/google")
    public ResponseEntity<?> loginConGoogle(@RequestBody Map<String, String> body) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Falta el token de Google"));
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token de Google inválido o expirado"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String correo = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();

            if (correo == null || (emailVerified != null && !emailVerified)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "El correo de Google no está verificado"));
            }

            Optional<Persona> personaOpt = personaRepository.findByCorreo(correo);
            if (personaOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acceso denegado: este correo no está registrado en la natillera"));
            }

            Persona admin = personaOpt.get();
            if (adminRoleId != null && !adminRoleId.isBlank()) {
                Long expectedRole = Long.parseLong(adminRoleId.trim());
                if (!expectedRole.equals(admin.getIdRol())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Acceso denegado: el usuario no tiene permisos de administrador"));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "id", admin.getId(),
                    "nombre", admin.getNombre(),
                    "apellido", admin.getApellido(),
                    "correo", admin.getCorreo(),
                    "idRol", admin.getIdRol()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al validar autenticación con Google"));
        }
    }
}