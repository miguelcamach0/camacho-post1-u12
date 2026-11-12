package com.empresa.reembolsos.infraestructura;

import com.empresa.reembolsos.dominio.EstadoSolicitud;
import com.empresa.reembolsos.dominio.SolicitudReembolso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioSolicitudesJpa
        extends JpaRepository<SolicitudReembolso, Long> {

    List<SolicitudReembolso> findByEmpleadoIdAndEstado(
            String empleadoId,
            EstadoSolicitud estado);
}

