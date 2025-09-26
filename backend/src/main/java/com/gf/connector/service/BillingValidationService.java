package com.gf.connector.service;

import com.gf.connector.domain.Transaction;
import com.gf.connector.facturante.model.CrearComprobanteRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio para validar datos de facturación antes de enviarlos a Facturante.
 * 
 * Responsabilidades:
 * 1. Validar datos de transacciones antes de generar facturas
 * 2. Validar requests de Facturante antes de enviarlos
 * 3. Aplicar reglas de negocio específicas de Argentina
 * 4. Proporcionar mensajes de error detallados
 */
@Service
@RequiredArgsConstructor
public class BillingValidationService {
    private static final Logger log = LoggerFactory.getLogger(BillingValidationService.class);
    
    // Patrones de validación
    private static final Pattern CUIT_PATTERN = Pattern.compile("^\\d{11}$");
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{7,8}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // Límites de facturación
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");
    
    /**
     * Valida una transacción antes de generar la factura
     */
    public ValidationResult validateTransaction(Transaction transaction) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        log.debug("Validando transacción: {}", transaction.getExternalId());
        
        // Validaciones obligatorias
        if (transaction.getExternalId() == null || transaction.getExternalId().trim().isEmpty()) {
            errors.add("ID externo de transacción es obligatorio");
        }
        
        if (transaction.getAmount() == null) {
            errors.add("Monto de transacción es obligatorio");
        } else {
            if (transaction.getAmount().compareTo(MIN_AMOUNT) < 0) {
                errors.add("Monto debe ser mayor a " + MIN_AMOUNT);
            }
            if (transaction.getAmount().compareTo(MAX_AMOUNT) > 0) {
                errors.add("Monto excede el límite máximo de " + MAX_AMOUNT);
            }
        }
        
        if (transaction.getCurrency() == null || transaction.getCurrency().trim().isEmpty()) {
            errors.add("Moneda es obligatoria");
        } else if (!"ARS".equals(transaction.getCurrency()) && !"BRL".equals(transaction.getCurrency())) {
            warnings.add("Moneda no estándar: " + transaction.getCurrency() + ". Se esperaba ARS o BRL");
        }
        
        // Validar documento del cliente
        String customerDoc = transaction.getCustomerDoc();
        if (customerDoc != null && !customerDoc.trim().isEmpty()) {
            String doc = customerDoc.replaceAll("[^0-9]", "");
            if (!isValidCUIT(doc) && !isValidDNI(doc)) {
                // Si el documento no es válido, permitir usar consumidor final
                warnings.add("Documento del cliente no es válido, se usará consumidor final");
            }
        }
        // Nota: Si customerDoc es null o vacío, se usará automáticamente consumidor final
        
        // Validar estado de transacción
        if (transaction.getStatus() == null) {
            errors.add("Estado de transacción es obligatorio");
        }
        
        log.debug("Validación de transacción completada. Errores: {}, Warnings: {}", errors.size(), warnings.size());
        
        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }
    
    /**
     * Valida un request de Facturante antes de enviarlo
     */
    public ValidationResult validateFacturanteRequest(CrearComprobanteRequest request) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        log.debug("Validando request de Facturante");
        
        // Validar autenticación
        if (request.getAutenticacion() == null) {
            errors.add("Datos de autenticación son obligatorios");
        } else {
            if (request.getAutenticacion().getEmpresa() == null || request.getAutenticacion().getEmpresa().trim().isEmpty()) {
                errors.add("Empresa en autenticación es obligatoria");
            }
            if (request.getAutenticacion().getUsuario() == null || request.getAutenticacion().getUsuario().trim().isEmpty()) {
                errors.add("Usuario en autenticación es obligatorio");
            }
            if (request.getAutenticacion().getHash() == null || request.getAutenticacion().getHash().trim().isEmpty()) {
                errors.add("Hash en autenticación es obligatorio");
            }
        }
        
        // Validar cliente
        if (request.getCliente() == null) {
            errors.add("Datos del cliente son obligatorios");
        } else {
            validateClienteData(request.getCliente(), errors, warnings);
        }
        
        // Validar encabezado
        if (request.getEncabezado() == null) {
            errors.add("Encabezado del comprobante es obligatorio");
        } else {
            validateEncabezadoData(request.getEncabezado(), errors, warnings);
        }
        
        // Validar items
        if (request.getItems() == null || request.getItems().length == 0) {
            errors.add("Al menos un item es obligatorio");
        } else {
            validateItemsData(request.getItems(), errors, warnings);
        }
        
        log.debug("Validación de request Facturante completada. Errores: {}, Warnings: {}", errors.size(), warnings.size());
        
        return ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .build();
    }
    
    /**
     * Valida datos del cliente
     */
    private void validateClienteData(com.gf.connector.facturante.model.Cliente cliente, List<String> errors, List<String> warnings) {
        if (cliente.getRazonSocial() == null || cliente.getRazonSocial().trim().isEmpty()) {
            errors.add("Razón social del cliente es obligatoria");
        } else if (cliente.getRazonSocial().length() > 100) {
            errors.add("Razón social del cliente no puede exceder 100 caracteres");
        }
        
        if (cliente.getTipoDocumento() == null) {
            errors.add("Tipo de documento del cliente es obligatorio");
        } else if (cliente.getTipoDocumento() != 80 && cliente.getTipoDocumento() != 96 && cliente.getTipoDocumento() != 99) {
            warnings.add("Tipo de documento no estándar: " + cliente.getTipoDocumento());
        }
        
        if (cliente.getNroDocumento() == null || cliente.getNroDocumento().trim().isEmpty()) {
            errors.add("Número de documento del cliente es obligatorio");
        } else {
            String doc = cliente.getNroDocumento().replaceAll("[^0-9]", "");
            if (cliente.getTipoDocumento() == 80 && !isValidCUIT(doc)) {
                errors.add("CUIT del cliente no es válido");
            } else if (cliente.getTipoDocumento() == 96 && !isValidDNI(doc)) {
                errors.add("DNI del cliente no es válido");
            }
        }
        
        if (cliente.getMailFacturacion() != null && !cliente.getMailFacturacion().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(cliente.getMailFacturacion()).matches()) {
                errors.add("Email de facturación no es válido");
            }
        }
    }
    
    /**
     * Valida datos del encabezado
     */
    private void validateEncabezadoData(com.gf.connector.facturante.model.ComprobanteEncabezado encabezado, List<String> errors, List<String> warnings) {
        if (encabezado.getTipoComprobante() == null || encabezado.getTipoComprobante().trim().isEmpty()) {
            errors.add("Tipo de comprobante es obligatorio");
        }
        
        if (encabezado.getPrefijo() == null || encabezado.getPrefijo().trim().isEmpty()) {
            errors.add("Prefijo del comprobante es obligatorio");
        }
        
        if (encabezado.getCondicionVenta() == null || encabezado.getCondicionVenta() == 0) {
            errors.add("Condición de venta es obligatoria");
        }
        
        // Validar montos
        if (encabezado.getSubTotal() == null || encabezado.getSubTotal().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Subtotal debe ser mayor a cero");
        }
        
        if (encabezado.getTotal() == null || encabezado.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Total debe ser mayor a cero");
        }
        
        // Validar coherencia de montos
        if (encabezado.getSubTotal() != null && encabezado.getTotal() != null) {
            if (encabezado.getTotal().compareTo(encabezado.getSubTotal()) < 0) {
                errors.add("Total no puede ser menor al subtotal");
            }
        }
    }
    
    /**
     * Valida datos de los items
     */
    private void validateItemsData(com.gf.connector.facturante.model.ComprobanteItem[] items, List<String> errors, List<String> warnings) {
        BigDecimal totalCalculado = BigDecimal.ZERO;
        
        for (int i = 0; i < items.length; i++) {
            com.gf.connector.facturante.model.ComprobanteItem item = items[i];
            String prefix = "Item " + (i + 1) + ": ";
            
            if (item.getDetalle() == null || item.getDetalle().trim().isEmpty()) {
                errors.add(prefix + "Detalle es obligatorio");
            } else if (item.getDetalle().length() > 200) {
                errors.add(prefix + "Detalle no puede exceder 200 caracteres");
            }
            
            if (item.getCantidad() == null || item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(prefix + "Cantidad debe ser mayor a cero");
            }
            
            if (item.getPrecioUnitario() == null || item.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(prefix + "Precio unitario debe ser mayor a cero");
            }
            
            if (item.getTotal() == null || item.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(prefix + "Total debe ser mayor a cero");
            }
            
            // Validar coherencia de cálculos
            if (item.getCantidad() != null && item.getPrecioUnitario() != null && item.getTotal() != null) {
                BigDecimal totalEsperado = item.getCantidad().multiply(item.getPrecioUnitario());
                if (item.getTotal().subtract(totalEsperado).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    warnings.add(prefix + "Total calculado (" + totalEsperado + ") difiere del total declarado (" + item.getTotal() + ")");
                }
            }
            
            if (item.getTotal() != null) {
                totalCalculado = totalCalculado.add(item.getTotal());
            }
        }
    }
    
    /**
     * Valida si un CUIT es válido
     */
    private boolean isValidCUIT(String cuit) {
        if (cuit == null || !CUIT_PATTERN.matcher(cuit).matches()) {
            return false;
        }
        
        // Validación de dígito verificador del CUIT
        int[] multipliers = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cuit.charAt(i)) * multipliers[i];
        }
        
        int remainder = sum % 11;
        int checkDigit = remainder < 2 ? remainder : 11 - remainder;
        
        return checkDigit == Character.getNumericValue(cuit.charAt(10));
    }
    
    /**
     * Valida si un DNI es válido
     */
    private boolean isValidDNI(String dni) {
        return dni != null && DNI_PATTERN.matcher(dni).matches();
    }
    
    /**
     * Resultado de validación
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;
        
        private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
            this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        }
        
        public static ValidationResultBuilder builder() {
            return new ValidationResultBuilder();
        }
        
        // Getters
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        public String getErrorsAsString() {
            return String.join("; ", errors);
        }
        
        public String getWarningsAsString() {
            return String.join("; ", warnings);
        }
        
        public static class ValidationResultBuilder {
            private boolean valid;
            private List<String> errors;
            private List<String> warnings;
            
            public ValidationResultBuilder valid(boolean valid) {
                this.valid = valid;
                return this;
            }
            
            public ValidationResultBuilder errors(List<String> errors) {
                this.errors = errors;
                return this;
            }
            
            public ValidationResultBuilder warnings(List<String> warnings) {
                this.warnings = warnings;
                return this;
            }
            
            public ValidationResult build() {
                return new ValidationResult(valid, errors, warnings);
            }
        }
    }
}