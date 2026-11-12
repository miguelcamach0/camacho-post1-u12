
package com.empresa.reembolsos.procesamiento.validacion;

import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Component;

import com.empresa.reembolsos.dominio.SolicitudReembolso;

@Component
public class ValidadorDuplicados
        extends ValidadorSolicitud {

    @Override
    protected ResultadoValidacion validarPropio(
            SolicitudReembolso solicitud,
            List<SolicitudReembolso> pendientes) {

        boolean existeDuplicado = pendientes.stream()
                .anyMatch(p ->
                    p.getCategoria() == solicitud.getCategoria()
                    && YearMonth.from(p.getFechaGasto())
                        .equals(YearMonth.from(
                            solicitud.getFechaGasto()))
                );

        return existeDuplicado
                ? ResultadoValidacion.rechazar(
                    "Ya existe una solicitud pendiente "
                    + "de la misma categoria en el periodo")
                : ResultadoValidacion.ok();
    }
}

