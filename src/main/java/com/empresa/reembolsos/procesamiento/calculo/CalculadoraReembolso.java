
package com.empresa.reembolsos.procesamiento.calculo;

import java.math.BigDecimal;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

public interface CalculadoraReembolso {

    /**
     * Identifica la categoria atendida por la estrategia.
     *
     * @return categoria de gasto
     */
    CategoriaGasto getCategoria();

    /**
     * Calcula el monto aprobado de una solicitud.
     *
     * @param solicitud solicitud de reembolso
     * @return monto aprobado
     */
    BigDecimal calcular(SolicitudReembolso solicitud);
}

