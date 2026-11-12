package com.empresa.reembolsos;

import com.empresa.reembolsos.infraestructura.RepositorioSolicitudesJpa;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class SolicitudReembolsoIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositorioSolicitudesJpa repositorio;

    @BeforeEach
    void limpiarDatos() {
        repositorio.deleteAll();
    }

    private String solicitudJson(
            String empleado,
            String categoria,
            String monto,
            int dias,
            boolean documentos) {

        return """
            {
              "empleadoId": "%s",
              "categoria": "%s",
              "montoSolicitado": %s,
              "fechaGasto": "2026-11-11",
              "diasViaje": %d,
              "documentosSoporte": %s
            }
            """.formatted(
                empleado, categoria, monto,
                dias, documentos);
    }

    @Test
    void creaSolicitudesEnLasTresCategorias()
            throws Exception {

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitudJson(
                        "EMP001", "VIAJE",
                        "400000", 3, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.montoAprobado").value(400000))
                .andExpect(jsonPath(
                        "$.estado").value("PENDIENTE"));

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitudJson(
                        "EMP002", "REPRESENTACION",
                        "1500000", 0, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.montoAprobado").value(1500000));

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitudJson(
                        "EMP003", "CAPACITACION",
                        "2000000", 0, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.montoAprobado").value(1600000));

        assertEquals(3, repositorio.count());
    }

    @Test
    void rechazaSolicitudSinDocumentos()
            throws Exception {

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitudJson(
                        "EMP004", "VIAJE",
                        "100000", 1, false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Faltan documentos soporte"));

        assertEquals(0, repositorio.count());
    }

    @Test
    void rechazaSolicitudDuplicada()
            throws Exception {

        String solicitud = solicitudJson(
                "EMP005", "VIAJE",
                "100000", 1, true);

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitud))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitud))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Ya existe una solicitud pendiente de "
                            + "la misma categoria en el periodo"));

        assertEquals(1, repositorio.count());
    }

    @Test
    void aprobacionNotificaATresInteresados(
            CapturedOutput output) throws Exception {

        var respuesta = mockMvc.perform(
                post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitudJson(
                        "EMP006", "VIAJE",
                        "120000", 1, true)))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(
                respuesta.getResponse()
                        .getContentAsString())
                .get("id").asLong();

        mockMvc.perform(put(
                "/api/solicitudes/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"APROBADA\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado")
                        .value("APROBADA"));

        assertTrue(output.getAll()
                .contains("Notificando a EMP006"));

        assertTrue(output.getAll()
                .contains("Contabilidad: programar pago"));

        assertTrue(output.getAll()
                .contains("Auditoria: solicitud"));
    }

    @Test
    void rechazoNoNotificaPagoAContabilidad(
            CapturedOutput output) throws Exception {

        var respuesta = mockMvc.perform(
                post("/api/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(solicitudJson(
                        "EMP007", "VIAJE",
                        "100000", 1, true)))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(
                respuesta.getResponse()
                        .getContentAsString())
                .get("id").asLong();

        mockMvc.perform(put(
                "/api/solicitudes/{id}/estado", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"RECHAZADA\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado")
                        .value("RECHAZADA"));

        assertTrue(output.getAll()
                .contains("Notificando a EMP007"));

        assertTrue(output.getAll()
                .contains("Auditoria: solicitud"));

        assertFalse(output.getAll()
                .contains("Contabilidad: programar pago"));
    }
}

