package com.empresa.reembolsos.aplicacion;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;

import com.empresa.reembolsos.procesamiento.validacion.CadenaValidadores;
import com.empresa.reembolsos.procesamiento.calculo.SelectorCalculadora;
import com.empresa.reembolsos.procesamiento.notificacion.ObservadorCambioEstado;

import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SolicitudReembolsoService {

    private final CadenaValidadores validadores;
    private final SelectorCalculadora calculadoras;
    private final List<ObservadorCambioEstado> observadores;
    private final RepositorioSolicitudesJpa repositorio;

    public SolicitudReembolsoService(
            CadenaValidadores validadores,
            SelectorCalculadora calculadoras,
            List<ObservadorCambioEstado> observadores,
            RepositorioSolicitudesJpa repositorio) {

        this.validadores = validadores;
        this.calculadoras = calculadoras;
        this.observadores = observadores;
        this.repositorio = repositorio;
    }

    public SolicitudReembolso crear(
            SolicitudReembolso solicitud) {

        var pendientes =
                repositorio.findByEmpleadoIdAndEstado(
                    solicitud.getEmpleadoId(),
                    EstadoSolicitud.PENDIENTE);

        var resultado = validadores.validar(
                solicitud, pendientes);

        if (!resultado.valida()) {
            throw new SolicitudInvalidaException(
                    resultado.motivoRechazo());
        }

        solicitud.setId(null);

        solicitud.setMontoAprobado(
                calculadoras.obtener(
                        solicitud.getCategoria())
                        .calcular(solicitud));

        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        return repositorio.save(solicitud);
    }

    public SolicitudReembolso cambiarEstado(
            Long id,
            EstadoSolicitud nuevoEstado) {

        var solicitud = repositorio.findById(id)
                .orElseThrow(() ->
                    new SolicitudNoEncontradaException(id));

        var estadoAnterior = solicitud.getEstado();

        if (estadoAnterior == nuevoEstado) {
            return solicitud;
        }

        solicitud.setEstado(nuevoEstado);

        var guardada = repositorio.save(solicitud);

        observadores.forEach(observador ->
                observador.onCambioEstado(
                        guardada, estadoAnterior));

        return guardada;
    }
}

