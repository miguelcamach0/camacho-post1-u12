package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(OutputCaptureExtension.class)
class NotificadoresTest {

    private SolicitudReembolso crearSolicitud(
            EstadoSolicitud estado) {

        SolicitudReembolso solicitud =
                new SolicitudReembolso();

        solicitud.setId(1L);
        solicitud.setEmpleadoId("EMP001");
        solicitud.setMontoAprobado(
                new BigDecimal("450000"));
        solicitud.setEstado(estado);

        return solicitud;
    }

    private void notificarATodos(
            SolicitudReembolso solicitud,
            EstadoSolicitud anterior) {

        List<ObservadorCambioEstado> observadores =
                List.of(
                    new NotificadorEmpleado(),
                    new NotificadorContabilidad(),
                    new NotificadorAuditoria()
                );

        observadores.forEach(observador ->
                observador.onCambioEstado(
                        solicitud, anterior));
    }

    @Test
    void aprobacionNotificaATodos(
            CapturedOutput output) {

        var solicitud = crearSolicitud(
                EstadoSolicitud.APROBADA);

        notificarATodos(
                solicitud, EstadoSolicitud.PENDIENTE);

        assertTrue(output.getAll().contains(
                "Notificando a EMP001"));

        assertTrue(output.getAll().contains(
                "Contabilidad: programar pago de 450000"));

        assertTrue(output.getAll().contains(
                "Auditoria: solicitud 1 cambio de "
                + "PENDIENTE a APROBADA"));
    }

    @Test
    void rechazoNoGeneraAccionDeContabilidad(
            CapturedOutput output) {

        var solicitud = crearSolicitud(
                EstadoSolicitud.RECHAZADA);

        notificarATodos(
                solicitud, EstadoSolicitud.PENDIENTE);

        assertTrue(output.getAll().contains(
                "Notificando a EMP001"));

        assertFalse(output.getAll().contains(
                "Contabilidad: programar pago"));

        assertTrue(output.getAll().contains(
                "Auditoria: solicitud 1 cambio de "
                + "PENDIENTE a RECHAZADA"));
    }

    @Test
    void correccionNoGeneraAccionDeContabilidad(
            CapturedOutput output) {

        var solicitud = crearSolicitud(
                EstadoSolicitud.REQUIERE_CORRECCION);

        notificarATodos(
                solicitud, EstadoSolicitud.PENDIENTE);

        assertTrue(output.getAll().contains(
                "REQUIERE_CORRECCION"));

        assertFalse(output.getAll().contains(
                "Contabilidad: programar pago"));

        assertTrue(output.getAll().contains(
                "Auditoria: solicitud 1 cambio de "
                + "PENDIENTE a REQUIERE_CORRECCION"));
    }

    @Test
    void auditoriaRegistraCambioDeEstado(
            CapturedOutput output) {

        var solicitud = crearSolicitud(
                EstadoSolicitud.APROBADA);

        new NotificadorAuditoria().onCambioEstado(
                solicitud, EstadoSolicitud.PENDIENTE);

        assertTrue(output.getAll().contains(
                "Auditoria: solicitud 1 cambio de "
                + "PENDIENTE a APROBADA"));
    }
}

