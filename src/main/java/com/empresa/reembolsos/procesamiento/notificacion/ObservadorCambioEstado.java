package com.empresa.reembolsos.procesamiento.notificacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

public interface ObservadorCambioEstado {

    /**
     * Notifica un cambio de estado.
     *
     * @param solicitud solicitud con el estado actualizado
     * @param estadoAnterior estado previo al cambio
     */
    void onCambioEstado(
            SolicitudReembolso solicitud,
            EstadoSolicitud estadoAnterior);
}

