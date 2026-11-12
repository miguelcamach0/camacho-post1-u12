package com.empresa.reembolsos.procesamiento.calculo;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

class CalculadorasTest {

    private SolicitudReembolso crearSolicitud(
            CategoriaGasto categoria,
            String monto,
            int diasViaje) {

        SolicitudReembolso solicitud =
                new SolicitudReembolso();

        solicitud.setCategoria(categoria);
        solicitud.setMontoSolicitado(
                new BigDecimal(monto));
        solicitud.setDiasViaje(diasViaje);

        return solicitud;
    }

    private void verificarMonto(
            String esperado,
            BigDecimal obtenido) {

        assertEquals(0,
                new BigDecimal(esperado)
                        .compareTo(obtenido));
    }

    @Test
    void viajeDentroDelTope() {

        var solicitud = crearSolicitud(
                CategoriaGasto.VIAJE,
                "100000", 2);

        var resultado =
                new CalculadoraViaje().calcular(solicitud);

        verificarMonto("100000", resultado);
    }

    @Test
    void viajeAplicaTopePorDias() {

        var solicitud = crearSolicitud(
                CategoriaGasto.VIAJE,
                "500000", 3);

        var resultado =
                new CalculadoraViaje().calcular(solicitud);

        verificarMonto("450000", resultado);
    }

    @Test
    void representacionDentroDelTope() {

        var solicitud = crearSolicitud(
                CategoriaGasto.REPRESENTACION,
                "1500000", 0);

        var resultado =
                new CalculadoraRepresentacion()
                        .calcular(solicitud);

        verificarMonto("1500000", resultado);
    }

    @Test
    void representacionAplicaTopeMaximo() {

        var solicitud = crearSolicitud(
                CategoriaGasto.REPRESENTACION,
                "2500000", 0);

        var resultado =
                new CalculadoraRepresentacion()
                        .calcular(solicitud);

        verificarMonto("2000000", resultado);
    }

    @Test
    void capacitacionAplicaOchentaPorCiento() {

        var solicitud = crearSolicitud(
                CategoriaGasto.CAPACITACION,
                "2000000", 0);

        var resultado =
                new CalculadoraCapacitacion()
                        .calcular(solicitud);

        verificarMonto("1600000", resultado);
    }

    @Test
    void capacitacionRespetaTopeAnual() {

        var solicitud = crearSolicitud(
                CategoriaGasto.CAPACITACION,
                "4000000", 0);

        var resultado =
                new CalculadoraCapacitacion()
                        .calcular(solicitud);

        verificarMonto("3000000", resultado);
    }

    @Test
    void selectorEncuentraLasTresEstrategias() {

        var viaje = new CalculadoraViaje();
        var representacion =
                new CalculadoraRepresentacion();
        var capacitacion =
                new CalculadoraCapacitacion();

        var selector = new SelectorCalculadora(
                List.of(viaje, representacion, capacitacion));

        assertSame(viaje,
                selector.obtener(CategoriaGasto.VIAJE));

        assertSame(representacion,
                selector.obtener(
                        CategoriaGasto.REPRESENTACION));

        assertSame(capacitacion,
                selector.obtener(
                        CategoriaGasto.CAPACITACION));
    }

    @Test
    void selectorRechazaCategoriaNoRegistrada() {

        var selector = new SelectorCalculadora(
                List.of(new CalculadoraViaje()));

        var excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> selector.obtener(
                        CategoriaGasto.CAPACITACION));

        assertTrue(excepcion.getMessage()
                .contains("Categoria no soportada"));
    }
}

