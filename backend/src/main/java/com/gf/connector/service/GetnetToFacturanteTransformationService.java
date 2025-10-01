package com.gf.connector.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.config.FacturanteConfig;
import com.gf.connector.facturante.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * Servicio especializado en la transformación de datos entre webhooks de Getnet
 * y el formato requerido por Facturante para la emisión de facturas electrónicas.
 * 
 * Maneja diferentes formatos de payload de Getnet y los mapea correctamente
 * a los campos requeridos por la API de Facturante según normativas AFIP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetnetToFacturanteTransformationService {
    
    private final FacturanteConfig facturanteConfig;
    private final ObjectMapper objectMapper;
    private final BillingSettingsService billingSettingsService;
    
    /**
     * Transforma un payload de webhook de Getnet en una transacción del dominio
     */
    public Transaction transformWebhookToTransaction(String rawJson, Map<String, Object> payload) {
        try {
            log.info("Iniciando transformación de webhook a transacción");
            
            // Detectar formato del payload
            PayloadFormat format = detectPayloadFormat(payload);
            log.info("Formato de payload detectado: {}", format);
            
            Transaction.TransactionBuilder builder = Transaction.builder();
            
            switch (format) {
                case BONVINO_FORMAT:
                    return transformBonvinoFormat(payload, builder);
                case SIMPLE_FORMAT:
                    return transformSimpleFormat(payload, builder);
                case CASO_REAL_FORMAT:
                    return transformCasoRealFormat(payload, builder);
                default:
                    return transformGenericFormat(payload, builder);
            }
            
        } catch (Exception e) {
            log.error("Error al transformar webhook a transacción", e);
            throw new RuntimeException("Error en transformación de datos: " + e.getMessage(), e);
        }
    }
    
    /**
     * Transforma una transacción en un request de Facturante
     */
    public CrearComprobanteRequest transformTransactionToFacturanteRequest(Transaction transaction, String originalPayload) {
        try {
            log.info("Transformando transacción {} a request de Facturante", transaction.getExternalId());
            
            // Parsear payload original para obtener datos adicionales
            JsonNode payloadNode = objectMapper.readTree(originalPayload);
            
            // Construir request
            CrearComprobanteRequest request = new CrearComprobanteRequest();
            request.setAutenticacion(buildAutenticacion());
            request.setCliente(buildCliente(transaction, payloadNode));
            request.setEncabezado(buildEncabezado(transaction));
            request.setItems(buildItems(transaction, payloadNode));
            
            log.info("Request de Facturante construido exitosamente");
            return request;
            
        } catch (Exception e) {
            log.error("Error al transformar transacción a request de Facturante", e);
            throw new RuntimeException("Error en transformación a Facturante: " + e.getMessage(), e);
        }
    }
    
    /**
     * Detecta el formato del payload basado en la estructura de campos
     */
    private PayloadFormat detectPayloadFormat(Map<String, Object> payload) {
        if (payload.containsKey("event_type") && payload.containsKey("data")) {
            return PayloadFormat.BONVINO_FORMAT;
        }
        if (payload.containsKey("metadata") && 
            payload.get("metadata") instanceof Map &&
            ((Map<?, ?>) payload.get("metadata")).containsKey("items")) {
            return PayloadFormat.CASO_REAL_FORMAT;
        }
        if (payload.containsKey("id") && payload.containsKey("status") && payload.containsKey("amount")) {
            return PayloadFormat.SIMPLE_FORMAT;
        }
        return PayloadFormat.GENERIC;
    }
    
    /**
     * Transforma formato Bonvino (estructura compleja con event_type y data)
     */
    private Transaction transformBonvinoFormat(Map<String, Object> payload, Transaction.TransactionBuilder builder) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) payload.get("data");
        
        String paymentId = (String) data.get("payment_id");
        Object amountObj = data.get("amount");
        String currency = (String) data.get("currency");
        String status = (String) data.get("status");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) data.get("customer");
        String customerDoc = customer != null ? (String) customer.get("document_number") : null;
        
        // Convertir amount de centavos a unidades monetarias si es necesario
        BigDecimal amount = convertAmount(amountObj, currency);
        
        return builder
                .externalId(paymentId)
                .amount(amount)
                .currency("ARS") // Convertir a ARS para Argentina
                .status(mapStatus(status))
                .customerDoc(customerDoc)
                .build();
    }
    
    /**
     * Transforma formato simple (estructura básica con campos directos)
     */
    private Transaction transformSimpleFormat(Map<String, Object> payload, Transaction.TransactionBuilder builder) {
        String id = (String) payload.get("id");
        String status = (String) payload.get("status");
        Object amountObj = payload.get("amount");
        String currency = (String) payload.get("currency");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> customer = (Map<String, Object>) payload.get("customer");
        String customerDoc = customer != null ? (String) customer.get("document") : null;
        
        BigDecimal amount = convertAmount(amountObj, currency);
        
        return builder
                .externalId(id)
                .amount(amount)
                .currency(currency != null ? currency : "ARS")
                .status(mapStatus(status))
                .customerDoc(customerDoc)
                .build();
    }
    
    /**
     * Transforma formato caso real (estructura con metadata e items)
     */
    private Transaction transformCasoRealFormat(Map<String, Object> payload, Transaction.TransactionBuilder builder) {
        String id = (String) payload.get("id");
        Object amountObj = payload.get("amount");
        String currency = (String) payload.get("currency");
        String status = (String) payload.get("status");
        String customerDoc = (String) payload.get("customerDoc");
        
        BigDecimal amount = convertAmount(amountObj, currency);
        
        return builder
                .externalId(id)
                .amount(amount)
                .currency(currency != null ? currency : "ARS")
                .status(mapStatus(status))
                .customerDoc(customerDoc)
                .build();
    }
    
    /**
     * Transforma formato genérico (fallback)
     */
    private Transaction transformGenericFormat(Map<String, Object> payload, Transaction.TransactionBuilder builder) {
        String id = String.valueOf(payload.getOrDefault("transaction_id", payload.getOrDefault("id", "UNKNOWN")));
        String status = String.valueOf(payload.getOrDefault("status", "paid"));
        Object amountObj = payload.getOrDefault("amount", "0");
        String currency = String.valueOf(payload.getOrDefault("currency", "ARS"));
        String customerDoc = String.valueOf(payload.getOrDefault("customer_doc", ""));
        
        BigDecimal amount = convertAmount(amountObj, currency);
        
        return builder
                .externalId(id)
                .amount(amount)
                .currency(currency)
                .status(mapStatus(status))
                .customerDoc(customerDoc.isEmpty() ? null : customerDoc)
                .build();
    }
    
    /**
     * Convierte el monto considerando diferentes formatos y monedas
     */
    private BigDecimal convertAmount(Object amountObj, String currency) {
        if (amountObj == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal amount;
        if (amountObj instanceof Number) {
            amount = new BigDecimal(amountObj.toString());
        } else {
            amount = new BigDecimal(String.valueOf(amountObj));
        }
        
        // Si es BRL y el monto es muy alto, probablemente esté en centavos
        if ("BRL".equals(currency) && amount.compareTo(new BigDecimal("1000")) > 0) {
            amount = amount.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        
        // Convertir BRL a ARS (tasa aproximada para demo)
        if ("BRL".equals(currency)) {
            amount = amount.multiply(new BigDecimal("150")); // 1 BRL ≈ 150 ARS
        }
        
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Mapea estados de Getnet a estados internos
     */
    private TransactionStatus mapStatus(String getnetStatus) {
        if (getnetStatus == null) {
            return TransactionStatus.FAILED;
        }
        
        switch (getnetStatus.toUpperCase()) {
            case "PAID":
            case "APPROVED":
            case "CAPTURED":
                return TransactionStatus.PAID;
            case "AUTHORIZED":
                return TransactionStatus.AUTHORIZED;
            case "REFUNDED":
                return TransactionStatus.REFUNDED;
            case "FAILED":
            case "REJECTED":
            case "CANCELLED":
                return TransactionStatus.FAILED;
            default:
                return TransactionStatus.AUTHORIZED;
        }
    }
    
    /**
     * Construye la autenticación para Facturante
     */
    private Autenticacion buildAutenticacion() {
        Autenticacion auth = new Autenticacion();
        auth.setEmpresa(facturanteConfig.getEmpresa());
        auth.setUsuario(facturanteConfig.getUsuario());
        auth.setHash(facturanteConfig.getPassword());
        return auth;
    }
    
    /**
     * Construye los datos del cliente para Facturante
     */
    private Cliente buildCliente(Transaction transaction, JsonNode payloadNode) {
        Cliente cliente = new Cliente();
        
        // Obtener configuración activa del tenant de la transacción
        var settings = billingSettingsService.getActiveSettings(transaction.getTenantId()).orElse(null);
        
        // Extraer datos del cliente del payload original
        String customerName = extractCustomerName(payloadNode);
        String customerEmail = extractCustomerEmail(payloadNode);
        String customerDoc = transaction.getCustomerDoc();
        
        // Configurar cliente usando configuración persistente con valores por defecto
        boolean useConsumidorFinal = (settings != null && Boolean.TRUE.equals(settings.getConsumidorFinalPorDefecto())) || 
                                   (customerDoc == null || customerDoc.trim().isEmpty());
        
        if (useConsumidorFinal) {
            // Usar configuración de consumidor final o valores por defecto
            if (settings != null) {
                cliente.setRazonSocial(settings.getRazonSocialConsumidorFinal() != null ? 
                    settings.getRazonSocialConsumidorFinal() : "Consumidor Final");
                cliente.setTipoDocumento(Integer.parseInt(settings.getTipoDocumentoCliente(
                    settings.getCuitConsumidorFinal() != null ? settings.getCuitConsumidorFinal() : "00000000000")));
                cliente.setNroDocumento(settings.getCuitConsumidorFinal() != null ? 
                    settings.getCuitConsumidorFinal() : "00000000000");
            } else {
                // Valores por defecto cuando no hay configuración
                cliente.setRazonSocial("Consumidor Final");
                cliente.setTipoDocumento(99); // Consumidor Final
                cliente.setNroDocumento("00000000000");
            }
        } else {
            // Usar datos del cliente
            cliente.setRazonSocial(customerName != null ? customerName : "Consumidor Final");
            
            if (settings != null) {
                cliente.setTipoDocumento(Integer.parseInt(settings.getTipoDocumentoCliente(customerDoc)));
            } else {
                // Fallback a lógica original
                if (customerDoc != null && customerDoc.length() == 11 && customerDoc.startsWith("20")) {
                    cliente.setTipoDocumento(80); // CUIT
                } else if (customerDoc != null && customerDoc.length() == 8) {
                    cliente.setTipoDocumento(96); // DNI
                } else {
                    cliente.setTipoDocumento(99); // Consumidor Final
                }
            }
            cliente.setNroDocumento(customerDoc != null ? customerDoc : "00000000");
        }
        
        // Email de facturación
        if (settings != null && settings.getEmailFacturacion() != null && !settings.getEmailFacturacion().trim().isEmpty()) {
            cliente.setMailFacturacion(settings.getEmailFacturacion());
        } else {
            cliente.setMailFacturacion(customerEmail != null ? customerEmail : "cliente@ejemplo.com");
        }
        
        cliente.setEnviarComprobante(settings != null && Boolean.TRUE.equals(settings.getEnviarComprobante()));
        
        return cliente;
    }
    
    /**
     * Construye el encabezado del comprobante
     */
    private ComprobanteEncabezado buildEncabezado(Transaction transaction) {
        ComprobanteEncabezado encabezado = new ComprobanteEncabezado();
        
        // Obtener configuración activa del tenant de la transacción
        var settings = billingSettingsService.getActiveSettings(transaction.getTenantId()).orElse(null);
        
        // Usar configuración persistente o fallback a configuración por defecto
        String tipoComprobante = null;
        String prefijo = null;
        
        if (settings != null) {
            tipoComprobante = settings.getTipoComprobante();
            prefijo = settings.getPuntoVenta();
        }
        
        // Si no hay configuración o los valores están vacíos, usar fallback
        if (tipoComprobante == null || tipoComprobante.trim().isEmpty()) {
            tipoComprobante = (facturanteConfig != null) ? facturanteConfig.getTipoComprobante() : "FB";
        }
        if (prefijo == null || prefijo.trim().isEmpty()) {
            prefijo = (facturanteConfig != null) ? facturanteConfig.getPrefijo() : "0001";
        }
        
        // Asegurar valores por defecto si aún están vacíos
        if (tipoComprobante == null || tipoComprobante.trim().isEmpty()) {
            tipoComprobante = "FB"; // Factura B por defecto
        }
        if (prefijo == null || prefijo.trim().isEmpty()) {
            prefijo = "0001"; // Punto de venta por defecto
        }
        
        encabezado.setTipoComprobante(tipoComprobante);
        encabezado.setPrefijo(prefijo);
        
        encabezado.setCondicionVenta(1); // Contado
        encabezado.setBienes(2); // Servicios
        encabezado.setFechaHora(new GregorianCalendar());
        
        // Calcular totales con IVA configurable
        BigDecimal total = transaction.getAmount();
        BigDecimal ivaPorcentaje = (settings != null && settings.getIvaPorDefecto() != null) 
            ? settings.getIvaPorDefecto() 
            : new BigDecimal("21.00");
        BigDecimal iva = total.multiply(ivaPorcentaje.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                             .setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = total.subtract(iva);
        
        encabezado.setSubTotal(total);
        encabezado.setTotalNeto(neto);
        encabezado.setTotal(total);
        encabezado.setPercepciones(BigDecimal.ZERO);
        
        return encabezado;
    }
    
    /**
     * Construye los items del comprobante
     */
    private ComprobanteItem[] buildItems(Transaction transaction, JsonNode payloadNode) {
        List<ComprobanteItem> items = new ArrayList<>();
        
        // Intentar extraer items del payload
        JsonNode itemsNode = extractItemsFromPayload(payloadNode);
        
        if (itemsNode != null && itemsNode.isArray() && itemsNode.size() > 0) {
            // Procesar items individuales
            for (JsonNode itemNode : itemsNode) {
                ComprobanteItem item = buildItemFromNode(itemNode);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        
        // Si no hay items específicos, crear uno genérico
        if (items.isEmpty()) {
            ComprobanteItem item = new ComprobanteItem();
            item.setCodigo("GETNET-" + transaction.getExternalId());
            item.setDetalle("Venta POS Getnet #" + transaction.getExternalId());
            item.setCantidad(BigDecimal.ONE);
            
            BigDecimal total = transaction.getAmount();
            BigDecimal iva = total.multiply(new BigDecimal("0.21")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal neto = total.subtract(iva);
            
            item.setPrecioUnitario(neto);
            item.setIva(new BigDecimal("21"));
            item.setGravado(true);
            item.setTotal(neto);
            
            items.add(item);
        }
        
        return items.toArray(new ComprobanteItem[0]);
    }
    
    /**
     * Extrae el nombre del cliente del payload
     */
    private String extractCustomerName(JsonNode payloadNode) {
        // Bonvino format
        JsonNode dataNode = payloadNode.get("data");
        if (dataNode != null) {
            JsonNode customerNode = dataNode.get("customer");
            if (customerNode != null && customerNode.has("name")) {
                return customerNode.get("name").asText();
            }
        }
        
        // Simple format
        JsonNode customerNode = payloadNode.get("customer");
        if (customerNode != null && customerNode.has("name")) {
            return customerNode.get("name").asText();
        }
        
        // Caso real format
        JsonNode metadataNode = payloadNode.get("metadata");
        if (metadataNode != null && metadataNode.has("customerName")) {
            return metadataNode.get("customerName").asText();
        }
        
        return null;
    }
    
    /**
     * Extrae el email del cliente del payload
     */
    private String extractCustomerEmail(JsonNode payloadNode) {
        // Bonvino format
        JsonNode dataNode = payloadNode.get("data");
        if (dataNode != null) {
            JsonNode customerNode = dataNode.get("customer");
            if (customerNode != null && customerNode.has("email")) {
                return customerNode.get("email").asText();
            }
        }
        
        // Simple format
        JsonNode customerNode = payloadNode.get("customer");
        if (customerNode != null && customerNode.has("email")) {
            return customerNode.get("email").asText();
        }
        
        // Caso real format
        JsonNode metadataNode = payloadNode.get("metadata");
        if (metadataNode != null && metadataNode.has("customerEmail")) {
            return metadataNode.get("customerEmail").asText();
        }
        
        return null;
    }
    
    /**
     * Extrae los items del payload
     */
    private JsonNode extractItemsFromPayload(JsonNode payloadNode) {
        // Caso real format
        JsonNode metadataNode = payloadNode.get("metadata");
        if (metadataNode != null && metadataNode.has("items")) {
            return metadataNode.get("items");
        }
        
        // Bonvino format
        JsonNode dataNode = payloadNode.get("data");
        if (dataNode != null) {
            JsonNode metadataSubNode = dataNode.get("metadata");
            if (metadataSubNode != null && metadataSubNode.has("products")) {
                return metadataSubNode.get("products");
            }
        }
        
        return null;
    }
    
    /**
     * Construye un item desde un nodo JSON
     */
    private ComprobanteItem buildItemFromNode(JsonNode itemNode) {
        try {
            ComprobanteItem item = new ComprobanteItem();
            
            // Extraer datos del item
            String name = itemNode.has("name") ? itemNode.get("name").asText() : 
                         itemNode.has("detalle") ? itemNode.get("detalle").asText() : "Producto";
            String sku = itemNode.has("sku") ? itemNode.get("sku").asText() : 
                        itemNode.has("codigo") ? itemNode.get("codigo").asText() : "ITEM-001";
            
            BigDecimal quantity = itemNode.has("quantity") ? new BigDecimal(itemNode.get("quantity").asText()) :
                                 itemNode.has("cantidad") ? new BigDecimal(itemNode.get("cantidad").asText()) : BigDecimal.ONE;
            
            BigDecimal unitPrice = itemNode.has("unit_price") ? new BigDecimal(itemNode.get("unit_price").asText()) :
                                  itemNode.has("unitPrice") ? new BigDecimal(itemNode.get("unitPrice").asText()) :
                                  itemNode.has("precioUnitario") ? new BigDecimal(itemNode.get("precioUnitario").asText()) : BigDecimal.ZERO;
            
            // Calcular totales con IVA
            BigDecimal totalBruto = unitPrice.multiply(quantity);
            BigDecimal iva = totalBruto.multiply(new BigDecimal("0.21")).setScale(2, RoundingMode.HALF_UP);
            BigDecimal neto = totalBruto.subtract(iva);
            
            item.setCodigo(sku);
            item.setDetalle(name);
            item.setCantidad(quantity);
            item.setPrecioUnitario(neto.divide(quantity, 2, RoundingMode.HALF_UP));
            item.setIva(new BigDecimal("21"));
            item.setGravado(true);
            item.setTotal(neto);
            
            return item;
            
        } catch (Exception e) {
            log.warn("Error al procesar item individual, se omitirá: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Enum para identificar diferentes formatos de payload
     */
    private enum PayloadFormat {
        BONVINO_FORMAT,
        SIMPLE_FORMAT,
        CASO_REAL_FORMAT,
        GENERIC
    }
}