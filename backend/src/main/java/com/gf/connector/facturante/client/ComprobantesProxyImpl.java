package com.gf.connector.facturante.client;

import com.gf.connector.facturante.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Slf4j
@Component
@ConditionalOnProperty(name = "facturante.production", havingValue = "false", matchIfMissing = true)
public class ComprobantesProxyImpl implements IComprobantesProxy {
    
    private final String serviceUrl;
    private final Random random = new Random();
    
    public ComprobantesProxyImpl() {
        this.serviceUrl = System.getenv().getOrDefault("FACTURANTE_SERVICE_URL", "https://testing.facturante.com/api/Comprobantes.svc");
    }
    
    public ComprobantesProxyImpl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    
    @Override
    public CrearComprobanteResponse crearComprobante(CrearComprobanteRequest request) throws Exception {
        try {
            log.info("Enviando request a Facturante: {}", serviceUrl);
            log.debug("Datos del comprobante - Empresa: {}, Usuario: {}, Tipo: {}", 
                request.getAutenticacion().getEmpresa(),
                request.getAutenticacion().getUsuario(),
                request.getEncabezado().getTipoComprobante());
            
            // Simular llamada HTTP/SOAP al servicio real
            // En una implementación real, aquí iría la lógica de comunicación SOAP
            Thread.sleep(500); // Simular latencia de red
            
            // Simular respuesta exitosa (90% de las veces)
            boolean exitoso = random.nextDouble() < 0.9;
            
            CrearComprobanteResponse response = new CrearComprobanteResponse();
            
            if (exitoso) {
                // Generar datos simulados pero realistas
                String numeroComprobante = request.getEncabezado().getPrefijo() + "-" + 
                    String.format("%08d", random.nextInt(99999999));
                String cae = String.valueOf(10000000000000L + random.nextLong() % 90000000000000L);
                
                response.setExitoso(true);
                response.setEstado("Aprobado");
                response.setCae(cae);
                response.setNumeroComprobante(numeroComprobante);
                response.setFechaVencimientoCae(LocalDateTime.now().plusYears(1)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                response.setPdfUrl("https://testing.facturante.com/pdf/" + numeroComprobante + ".pdf");
                response.setMensajes(new String[]{"Comprobante creado exitosamente"});
                
                log.info("Comprobante creado exitosamente: {} - CAE: {}", 
                    numeroComprobante, cae);
            } else {
                // Simular error
                response.setExitoso(false);
                response.setEstado("Rechazado");
                response.setMensajes(new String[]{
                    "Error de validación en AFIP", 
                    "Verifique los datos del comprobante"
                });
                
                log.warn("Comprobante rechazado por Facturante");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error al comunicarse con Facturante", e);
            
            CrearComprobanteResponse errorResponse = new CrearComprobanteResponse();
            errorResponse.setExitoso(false);
            errorResponse.setEstado("Error");
            errorResponse.setMensajes(new String[]{"Error de comunicación: " + e.getMessage()});
            
            return errorResponse;
        }
    }
}