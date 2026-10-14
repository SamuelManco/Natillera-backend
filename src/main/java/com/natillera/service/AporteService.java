package com.natillera.service;

import com.natillera.model.Movimiento;
import com.natillera.model.Persona;
import com.natillera.model.TipoMovimiento;
import com.natillera.repository.PersonaRepository;
import com.natillera.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class AporteService {

    private static final String TIPO_APORTE = "Aporte";

    @Autowired
    private TipoMovimientoRepository tipoMovimientoRepository;

    @Autowired
    private PersonaRepository personaRepository;

    /**
     * Si el movimiento es un Aporte, exige que el monto sea igual o mayor a la cuota mínima
     * con la que se registró el socio. Si no es un aporte, no hace nada.
     */
    public void validarAporte(Movimiento movimiento) {
        if (movimiento.getIdTipoMovimiento() == null) {
            return;
        }
        Optional<TipoMovimiento> tipoAporte = tipoMovimientoRepository.findByNombre(TIPO_APORTE);
        if (tipoAporte.isEmpty() || !tipoAporte.get().getId().equals(movimiento.getIdTipoMovimiento())) {
            return; // no es un aporte, la regla no aplica
        }

        if (movimiento.getIdPersona() == null) {
            throw new IllegalArgumentException("Un aporte debe pertenecer a un socio");
        }
        if (movimiento.getMonto() == null || movimiento.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto del aporte debe ser mayor a cero");
        }

        Persona persona = personaRepository.findById(movimiento.getIdPersona())
                .orElseThrow(() -> new IllegalArgumentException("No existe el socio del aporte"));

        Long cuota = persona.getCuotaMinima();
        if (cuota != null && movimiento.getMonto() < cuota) {
            throw new IllegalArgumentException("El aporte de " + persona.getNombre() + " " + persona.getApellido()
                    + " debe ser de al menos " + pesos(cuota) + " (su cuota mínima). Se intentó registrar "
                    + pesos(movimiento.getMonto()) + ".");
        }
    }

    private static String pesos(long valor) {
        return "$" + NumberFormat.getIntegerInstance(new Locale("es", "CO")).format(valor);
    }
}
