
package com.empresa.reembolsos.procesamiento.validacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

@Component
public class ValidadorMontoMaximo
        extends ValidadorSolicitud {

    private static final BigDecimal TOPE_DIARIO_VIAJE =
            new BigDecimal("150000");

    private static final Map<CategoriaGasto, BigDecimal> TOPES =
            Map.of(
                CategoriaGasto.REPRESENTACION,
                new BigDecimal("2000000"),

                CategoriaGasto.CAPACITACION,
                new BigDecimal("3000000")
            );

    @Override
    protected ResultadoValidacion validarPropio(
            SolicitudReembolso solicitud,
            List<SolicitudReembolso> pendientes) {

        if (solicitud.getCategoria() == null
                || solicitud.getMontoSolicitado() == null) {
            return ResultadoValidacion.rechazar(
                    "Categoria y monto son obligatorios");
        }

        if (solicitud.getMontoSolicitado()
                .compareTo(BigDecimal.ZERO) <= 0) {
            return ResultadoValidacion.rechazar(
                    "El monto solicitado debe ser positivo");
        }

        BigDecimal tope;

        if (solicitud.getCategoria()
                == CategoriaGasto.VIAJE) {

            if (solicitud.getDiasViaje() <= 0) {
                return ResultadoValidacion.rechazar(
                        "Los dias de viaje deben ser mayores a cero");
            }

            tope = TOPE_DIARIO_VIAJE.multiply(
                    BigDecimal.valueOf(
                            solicitud.getDiasViaje()));

        } else {
            tope = TOPES.get(solicitud.getCategoria());
        }

        if (tope == null) {
            return ResultadoValidacion.rechazar(
                    "Categoria no soportada");
        }

        return solicitud.getMontoSolicitado()
                .compareTo(tope) > 0
                ? ResultadoValidacion.rechazar(
                    "Monto excede el tope de la categoria "
                    + solicitud.getCategoria())
                : ResultadoValidacion.ok();
    }
}

