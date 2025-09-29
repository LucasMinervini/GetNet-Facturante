package com.gf.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.Invoice;
import com.gf.connector.domain.Transaction;
import com.gf.connector.facturante.model.CrearComprobanteRequest;
import com.gf.connector.facturante.model.CrearComprobanteResponse;
import com.gf.connector.facturante.service.FacturanteService;
import com.gf.connector.repo.InvoiceRepository;
import com.gf.connector.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final FacturanteService facturanteService;
    private final ObjectMapper objectMapper;
    private final BillingValidationService validationService;
    private final GetnetToFacturanteTransformationService transformationService;

    public Invoice createPendingInvoice(Transaction tx, String requestJson) {
        Invoice inv = Invoice.builder()
                .transaction(tx)
                .status("pending")
                .requestJson(requestJson)
                .build();
        return invoiceRepository.save(inv);
    }
    
    /**
     * Crea una factura en Facturante para la transacción dada
     */
    public Invoice createFacturaInFacturante(Transaction transaction) {
        log.info("Creando factura en Facturante para transacción: {}", transaction.getExternalId());

        // 1. Validar transacción antes de procesar
        BillingValidationService.ValidationResult transactionValidation = validationService.validateTransaction(transaction);
        if (!transactionValidation.isValid()) {
            log.error("Transacción no válida para facturación: {}", transactionValidation.getErrorsAsString());
            throw new IllegalArgumentException("Transacción no válida: " + transactionValidation.getErrorsAsString());
        }
        
        if (transactionValidation.hasWarnings()) {
            log.warn("Advertencias en validación de transacción: {}", transactionValidation.getWarningsAsString());
        }

        // 2. Crear invoice pendiente
        Invoice invoice = createPendingInvoice(transaction, "{}");

        try {
            // 3. Generar request de Facturante usando el servicio de transformación
            CrearComprobanteRequest facturanteRequest = transformationService.transformTransactionToFacturanteRequest(transaction, "{}");
            
            // 4. Validar request de Facturante antes de enviarlo
            BillingValidationService.ValidationResult requestValidation = validationService.validateFacturanteRequest(facturanteRequest);
            if (!requestValidation.isValid()) {
                log.error("Request de Facturante no válido: {}", requestValidation.getErrorsAsString());
                invoice.setStatus("error");
                invoice.setResponseJson("{\"error\": \"Validation failed\", \"details\": \"" + requestValidation.getErrorsAsString() + "\"}");
                return invoiceRepository.save(invoice);
            }
            
            if (requestValidation.hasWarnings()) {
                log.warn("Advertencias en validación de request Facturante: {}", requestValidation.getWarningsAsString());
            }
            
            // 5. Guardar request JSON para auditoría
            invoice.setRequestJson(objectMapper.writeValueAsString(facturanteRequest));
            invoiceRepository.save(invoice);

            // 6. Llamar a Facturante
            log.info("Enviando request validado a Facturante para transacción: {}", transaction.getExternalId());
            CrearComprobanteResponse response = facturanteService.crearFactura(transaction);

            // 7. Procesar respuesta
            if (response.getExitoso()) {
                invoice.setStatus("sent");
                invoice.setPdfUrl(response.getPdfUrl());
                
                // Actualizar transaction con datos de factura
                transaction.setCae(response.getCae());
                transaction.setInvoiceNumber(response.getNumeroComprobante());
                // Guardar también la URL directa del PDF en la transacción para que el frontend pueda abrir Facturante
                transaction.setInvoicePdfUrl(response.getPdfUrl());
                transactionRepository.save(transaction);
                
                log.info("Factura creada exitosamente: CAE={}, Número={}, PDF={}", 
                        response.getCae(), response.getNumeroComprobante(), response.getPdfUrl());
            } else {
                invoice.setStatus("error");
                
            }
            
            // 8. Guardar response JSON
            invoice.setResponseJson(objectMapper.writeValueAsString(response));
            
        } catch (IllegalArgumentException e) {
            // Error de validación - no reintentar
            invoice.setStatus("error");
            log.error("Error de validación al crear factura: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            // Error técnico - podría reintentarse
            invoice.setStatus("error");
            log.error("Excepción técnica al crear factura en Facturante para transacción: {}", transaction.getExternalId(), e);
            
            try {
                invoice.setResponseJson(objectMapper.writeValueAsString(Map.of(
                    "error", "Technical error",
                    "message", e.getMessage(),
                    "type", e.getClass().getSimpleName()
                )));
            } catch (Exception jsonError) {
                log.error("Error adicional al serializar error response", jsonError);
            }
        }

        return invoiceRepository.save(invoice);
    }
}
