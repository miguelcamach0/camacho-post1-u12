
package com.empresa.reembolsos.procesamiento.validacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.empresa.reembolsos.dominio.CategoriaGasto;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

class CadenaValidadoresTest {

    private CadenaValidadores cadena;

    @BeforeEach
    void configurar() {
        cadena = new CadenaValidadores(
                new ValidadorDocumentosSoporte(),
                new ValidadorMontoMaximo(),
                new ValidadorDuplicados()
        );
    }

    private SolicitudReembolso crearSolicitud(
            String empleado,
            CategoriaGasto categoria,
            String monto,
            boolean documentos,
            LocalDate fecha) {

        SolicitudReembolso solicitud =
                new SolicitudReembolso();

        solicitud.setEmpleadoId(empleado);
        solicitud.setCategoria(categoria);
        solicitud.setMontoSolicitado(
                new BigDecimal(monto));
        solicitud.setDocumentosSoporte(documentos);
        solicitud.setFechaGasto(fecha);

        return solicitud;
    }

    @Test
    void solicitudValidaSuperaTodasLasVerificaciones() {

        var solicitud = crearSolicitud(
                "EMP001",
                CategoriaGasto.VIAJE,
                "100000",
                true,
                LocalDate.of(2026, 11, 10));

        var resultado = cadena.validar(
                solicitud, List.of());

        assertTrue(resultado.valida());
        assertNull(resultado.motivoRechazo());
    }

    @Test
    void rechazaSolicitudSinDocumentos() {

        var solicitud = crearSolicitud(
                "EMP001",
                CategoriaGasto.VIAJE,
                "100000",
                false,
                LocalDate.of(2026, 11, 10));

        var resultado = cadena.validar(
                solicitud, List.of());

        assertFalse(resultado.valida());
        assertEquals(
                "Faltan documentos soporte",
                resultado.motivoRechazo());
    }

    @Test
    void rechazaMontoSuperiorAlTope() {

        var solicitud = crearSolicitud(
                "EMP001",
                CategoriaGasto.REPRESENTACION,
                "2500000",
                true,
                LocalDate.of(2026, 11, 10));

        var resultado = cadena.validar(
                solicitud, List.of());

        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo()
                .contains("Monto excede"));
    }

    @Test
    void rechazaSolicitudDuplicada() {

        var solicitud = crearSolicitud(
                "EMP001",
                CategoriaGasto.VIAJE,
                "100000",
                true,
                LocalDate.of(2026, 11, 10));

        var pendiente = crearSolicitud(
                "EMP001",
                CategoriaGasto.VIAJE,
                "90000",
                true,
                LocalDate.of(2026, 11, 2));

        var resultado = cadena.validar(
                solicitud, List.of(pendiente));

        assertFalse(resultado.valida());
        assertTrue(resultado.motivoRechazo()
                .contains("solicitud pendiente"));
    }

    @Test
    void mismoMesDeOtroAnioNoEsDuplicado() {

        var solicitud = crearSolicitud(
                "EMP001",
                CategoriaGasto.VIAJE,
                "100000",
                true,
                LocalDate.of(2026, 11, 10));

        var pendienteAnterior = crearSolicitud(
                "EMP001",
                CategoriaGasto.VIAJE,
                "90000",
                true,
                LocalDate.of(2025, 11, 2));

        var resultado = cadena.validar(
                solicitud, List.of(pendienteAnterior));

        assertTrue(resultado.valida());
    }

    @Test
    void documentosSeValidanAntesDelMonto() {

        var solicitud = crearSolicitud(
                "EMP001",
                CategoriaGasto.REPRESENTACION,
                "2500000",
                false,
                LocalDate.of(2026, 11, 10));

        var resultado = cadena.validar(
                solicitud, List.of());

        assertFalse(resultado.valida());
        assertEquals(
                "Faltan documentos soporte",
                resultado.motivoRechazo());
    }
}

