package com.gf.connector.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.facturante.model.GetnetWebhookPayload;
import com.gf.connector.facturante.service.GetnetService;
import com.gf.connector.security.GetnetSignatureService;
import com.gf.connector.service.WebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final GetnetService getnetService;
    private final ObjectMapper objectMapper;
    private final GetnetSignatureService signatureService;

    @PostMapping("/getnet")
    public ResponseEntity<?> handleGetnet(@RequestBody String rawBody, HttpServletRequest request) throws Exception {
        String headerName = signatureService.getSignatureHeaderName();
        String signature = request.getHeader(headerName);
        
        // Validar firma HMAC
        if (!signatureService.verify(rawBody, signature)) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", "error",
                    "received", false,
                    "error", "invalid_signature",
                    "error_code", "INVALID_SIGNATURE",
                    "hint", "Ensure you send the correct HMAC in header '" + headerName + "'"
            ));
        }

        try {
            // Parse JSON payload como GetnetWebhookPayload
            GetnetWebhookPayload webhookPayload = objectMapper.readValue(rawBody, GetnetWebhookPayload.class);
            
            // Procesar webhook usando el servicio de GetNet
            getnetService.processWebhook(webhookPayload);
            
            // También procesar con el servicio de webhook existente para compatibilidad
            Map<String, Object> payload = objectMapper.readValue(rawBody, new TypeReference<>(){});
            WebhookService.WebhookProcessingResult result = webhookService.processGetnetPayload(rawBody, payload);

            if (result.isSuccess()) {
                // Respuesta exitosa con información detallada para debug
                Map<String, Object> debugInfo = new java.util.HashMap<>();
                debugInfo.put("payment_intent_id", webhookPayload.getPaymentIntentId());
                debugInfo.put("checkout_id", webhookPayload.getCheckoutId());
                debugInfo.put("order_id", webhookPayload.getOrderId());
                debugInfo.put("payment_status", webhookPayload.getPayment().getResult().getStatus());
                debugInfo.put("transaction_id", result.getTransaction() != null ? result.getTransaction().getId().toString() : null);
                debugInfo.put("external_id", result.getTransaction() != null ? result.getTransaction().getExternalId() : null);
                debugInfo.put("transaction_status", result.getTransaction() != null ? result.getTransaction().getStatus().toString() : null);
                debugInfo.put("invoice_generated", result.getInvoice() != null);
                debugInfo.put("invoice_id", result.getInvoice() != null ? result.getInvoice().getId().toString() : null);
                debugInfo.put("invoice_status", result.getInvoice() != null ? result.getInvoice().getStatus() : null);
                debugInfo.put("cae", result.getTransaction() != null ? result.getTransaction().getCae() : null);
                debugInfo.put("invoice_number", result.getTransaction() != null ? result.getTransaction().getInvoiceNumber() : null);
                
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "received", true,
                        "message", "Webhook de GetNet procesado exitosamente",
                        "debug", debugInfo
                ));
            } else {
                // Error en el procesamiento
                return ResponseEntity.status(500).body(Map.of(
                        "status", "error",
                        "received", false,
                        "message", result.getMessage(),
                        "error_code", "PROCESSING_ERROR",
                        "debug", Map.of(
                                "transaction_id", result.getTransaction() != null ? result.getTransaction().getId() : null,
                                "webhook_event_id", result.getWebhookEvent() != null ? result.getWebhookEvent().getId() : null
                        )
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "received", false,
                    "message", "Error parsing or processing webhook: " + e.getMessage(),
                    "error_code", "PARSING_ERROR"
            ));
        }
    }
}
