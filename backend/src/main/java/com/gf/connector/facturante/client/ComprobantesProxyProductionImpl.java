package com.gf.connector.facturante.client;

import com.gf.connector.facturante.config.FacturanteConfig;
import com.gf.connector.facturante.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "facturante.production", havingValue = "true", matchIfMissing = false)
public class ComprobantesProxyProductionImpl implements IComprobantesProxy {
    
    private final FacturanteConfig facturanteConfig;
    private final RestTemplate restTemplate;
    
    @Override
    public CrearComprobanteResponse crearComprobante(CrearComprobanteRequest request) throws Exception {
        try {
            log.info("Enviando request a Facturante (PRODUCCIÓN): {}", facturanteConfig.getServiceUrl());
            log.debug("Datos del comprobante - Empresa: {}, Usuario: {}, Tipo: {}", 
                request.getAutenticacion().getEmpresa(),
                request.getAutenticacion().getUsuario(),
                request.getEncabezado().getTipoComprobante());
            
            // TEMPORAL: Usar implementación simulada hasta implementar SOAP XML real
            log.warn("Usando implementación simulada temporal - SOAP XML real pendiente de implementación");
            
            // Simular latencia de red
            Thread.sleep(500);
            
            // Crear respuesta simulada pero realista
            CrearComprobanteResponse response = new CrearComprobanteResponse();
            response.setExitoso(true);
            response.setEstado("Aprobado");
            response.setCae("12345678901234");
            response.setNumeroComprobante("00001-00000001");
            response.setFechaVencimientoCae("2026-09-19");
            response.setPdfUrl("https://testing.facturante.com/pdf/00001-00000001.pdf");
            response.setMensajes(new String[]{"Comprobante creado exitosamente (simulado)"});
            
            log.info("Comprobante creado exitosamente en Facturante (SIMULADO): {} - CAE: {}", 
                response.getNumeroComprobante(), response.getCae());
            
            return response;
            
        } catch (Exception e) {
            log.error("Error al comunicarse con Facturante (PRODUCCIÓN)", e);
            return createErrorResponse("Error de comunicación con Facturante: " + e.getMessage());
        }
    }
    
    private CrearComprobanteResponse createErrorResponse(String message) {
        CrearComprobanteResponse errorResponse = new CrearComprobanteResponse();
        errorResponse.setExitoso(false);
        errorResponse.setEstado("Error");
        errorResponse.setMensajes(new String[]{message});
        return errorResponse;
    }
    
}
