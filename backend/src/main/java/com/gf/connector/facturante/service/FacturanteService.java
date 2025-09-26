package com.gf.connector.facturante.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.facturante.client.IComprobantesProxy;
import com.gf.connector.facturante.config.FacturanteConfig;
import com.gf.connector.facturante.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.GregorianCalendar;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturanteService {
    
    private final IComprobantesProxy comprobantesProxy;
    private final FacturanteConfig facturanteConfig;
    
    public CrearComprobanteResponse crearFactura(Transaction transaction) {
        try {
            log.info("Creando factura para transacción: {}", transaction.getExternalId());
            
            // Crear request
            CrearComprobanteRequest request = buildFacturaRequest(transaction);
            
            // Llamar al servicio
            CrearComprobanteResponse response = comprobantesProxy.crearComprobante(request);
            
            if (response.getExitoso()) {
                log.info("Factura creada exitosamente. CAE: {}, Número: {}", 
                    response.getCae(), response.getNumeroComprobante());
            } else {
                log.error("Error al crear factura: {}", String.join(", ", response.getMensajes()));
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error inesperado al crear factura", e);
            
            CrearComprobanteResponse errorResponse = new CrearComprobanteResponse();
            errorResponse.setExitoso(false);
            errorResponse.setEstado("Error");
            errorResponse.setMensajes(new String[]{"Error interno: " + e.getMessage()});
            
            return errorResponse;
        }
    }
    
    private CrearComprobanteRequest buildFacturaRequest(Transaction transaction) {
        // Autenticación
        Autenticacion auth = new Autenticacion();
        auth.setEmpresa(facturanteConfig.getEmpresa());
        auth.setUsuario(facturanteConfig.getUsuario());
        auth.setHash(facturanteConfig.getPassword());
        
        // Cliente (usando datos de la transacción o valores por defecto)
        Cliente cliente = new Cliente();
        cliente.setRazonSocial("Consumidor Final");
        cliente.setTipoDocumento(99); // 99 = Consumidor Final
        cliente.setNroDocumento(transaction.getCustomerDoc() != null ? 
            transaction.getCustomerDoc() : "00000000");
        cliente.setMailFacturacion("cliente@ejemplo.com");
        cliente.setEnviarComprobante(true);
        
        // Encabezado
        ComprobanteEncabezado encabezado = new ComprobanteEncabezado();
        encabezado.setTipoComprobante(facturanteConfig.getTipoComprobante());
        encabezado.setPrefijo(facturanteConfig.getPrefijo());
        encabezado.setCondicionVenta(1); // 1 = Contado
        encabezado.setBienes(2); // 2 = Servicios
        encabezado.setFechaHora(new GregorianCalendar());
        
        // Calcular totales
        BigDecimal amount = transaction.getAmount();
        BigDecimal iva = amount.multiply(new BigDecimal("0.21")); // 21% IVA
        BigDecimal neto = amount.subtract(iva);
        
        encabezado.setSubTotal(amount);
        encabezado.setTotalNeto(neto);
        encabezado.setTotal(amount);
        encabezado.setPercepciones(BigDecimal.ZERO);
        
        // Item
        ComprobanteItem item = new ComprobanteItem();
        item.setCodigo("GETNET-" + transaction.getExternalId());
        item.setDetalle("Venta POS Getnet #" + transaction.getExternalId());
        item.setCantidad(BigDecimal.ONE);
        item.setPrecioUnitario(neto);
        item.setIva(new BigDecimal("21"));
        item.setGravado(true);
        item.setTotal(neto);
        
        // Construir request
        CrearComprobanteRequest request = new CrearComprobanteRequest();
        request.setAutenticacion(auth);
        request.setCliente(cliente);
        request.setEncabezado(encabezado);
        request.setItems(new ComprobanteItem[]{item});
        
        return request;
    }
    
    /**
     * Crea una nota de crédito en Facturante para la transacción dada
     */
    public CrearComprobanteResponse crearNotaCredito(Transaction transaction) {
        try {
            log.info("Creando nota de crédito para transacción: {}", transaction.getExternalId());
            
            // Crear request
            CrearComprobanteRequest request = buildCreditNoteRequest(transaction);
            
            // Llamar al servicio
            CrearComprobanteResponse response = comprobantesProxy.crearComprobante(request);
            
            if (response.getExitoso()) {
                log.info("Nota de crédito creada exitosamente. CAE: {}, Número: {}", 
                    response.getCae(), response.getNumeroComprobante());
            } else {
                log.error("Error al crear nota de crédito: {}", String.join(", ", response.getMensajes()));
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error inesperado al crear nota de crédito", e);
            
            CrearComprobanteResponse errorResponse = new CrearComprobanteResponse();
            errorResponse.setExitoso(false);
            errorResponse.setEstado("Error");
            errorResponse.setMensajes(new String[]{"Error interno: " + e.getMessage()});
            
            return errorResponse;
        }
    }
    
    /**
     * Construye el request para nota de crédito
     */
    public CrearComprobanteRequest buildCreditNoteRequest(Transaction transaction) {
        // Autenticación
        Autenticacion auth = new Autenticacion();
        auth.setEmpresa(facturanteConfig.getEmpresa());
        auth.setUsuario(facturanteConfig.getUsuario());
        auth.setHash(facturanteConfig.getPassword());
        
        // Cliente (usando datos de la transacción o valores por defecto)
        Cliente cliente = new Cliente();
        cliente.setRazonSocial("Consumidor Final");
        cliente.setTipoDocumento(99); // 99 = Consumidor Final
        cliente.setNroDocumento(transaction.getCustomerDoc() != null ? 
            transaction.getCustomerDoc() : "00000000");
        cliente.setMailFacturacion("cliente@ejemplo.com");
        cliente.setEnviarComprobante(true);
        
        // Encabezado - Nota de crédito
        ComprobanteEncabezado encabezado = new ComprobanteEncabezado();
        encabezado.setTipoComprobante("NC"); // NC = Nota de Crédito
        encabezado.setPrefijo(facturanteConfig.getPrefijo());
        encabezado.setCondicionVenta(1); // 1 = Contado
        encabezado.setBienes(2); // 2 = Servicios
        encabezado.setFechaHora(new GregorianCalendar());
        
        // Calcular totales
        BigDecimal amount = transaction.getAmount();
        BigDecimal iva = amount.multiply(new BigDecimal("0.21")); // 21% IVA
        BigDecimal neto = amount.subtract(iva);
        
        encabezado.setSubTotal(amount);
        encabezado.setTotalNeto(neto);
        encabezado.setTotal(amount);
        encabezado.setPercepciones(BigDecimal.ZERO);
        
        // Item
        ComprobanteItem item = new ComprobanteItem();
        item.setCodigo("NC-GETNET-" + transaction.getExternalId());
        item.setDetalle("Reembolso POS Getnet #" + transaction.getExternalId());
        item.setCantidad(BigDecimal.ONE);
        item.setPrecioUnitario(neto);
        item.setIva(new BigDecimal("21"));
        item.setGravado(true);
        item.setTotal(neto);
        
        // Construir request
        CrearComprobanteRequest request = new CrearComprobanteRequest();
        request.setAutenticacion(auth);
        request.setCliente(cliente);
        request.setEncabezado(encabezado);
        request.setItems(new ComprobanteItem[]{item});
        
        return request;
    }
}