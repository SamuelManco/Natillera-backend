package com.natillera.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte las reglas de negocio que fallan en respuestas con mensaje claro,
 * en vez del "500 Internal Server Error" sin explicación.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Datos inválidos: monto negativo, fecha fuera de plazo, aporte menor a la cuota, etc.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> datosInvalidos(IllegalArgumentException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // El estado actual no permite la operación: rifa ya sorteada, préstamo ya pagado, catálogo faltante, etc.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> reglaDeNegocio(IllegalStateException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage());
    }

    // UNIQUE repetido (documento, correo) o registro con datos relacionados
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(DataIntegrityViolationException ex) {
        return respuesta(HttpStatus.CONFLICT,
                "No se pudo guardar: ya existe un registro con ese dato (por ejemplo, documento o correo repetido) "
                        + "o este registro está relacionado con otros datos.");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", mensaje);
        body.put("mensaje", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
