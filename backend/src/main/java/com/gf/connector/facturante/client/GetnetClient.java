package com.gf.connector.facturante.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.facturante.config.GetnetConfig;
import com.gf.connector.facturante.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetnetClient {
    
    private final RestTemplate restTemplate;
    private final GetnetConfig config;
    private final ObjectMapper objectMapper;
    
    private String accessToken;
    private long tokenExpiry;
    
    /**
     * Crea un Payment Intent en GetNet
     */
    public GetnetPaymentIntentResponse createPaymentIntent(GetnetPaymentIntent paymentIntent) {
        String url = config.getBaseUrl() + "/payment-intent";
        
        HttpHeaders headers = createAuthHeaders();
        
        // Log detallado de headers para debugging
        log.info("=== DEBUG PAYMENT INTENT ===");
        log.info("URL: {}", url);
        log.info("Headers: {}", headers);
        log.info("Token: {}", headers.getFirst("Authorization"));
        log.info("Seller ID: {}", headers.getFirst("x-seller-id"));
        log.info("Content-Type: {}", headers.getContentType());
        log.info("==========================");
        
        HttpEntity<GetnetPaymentIntent> request = new HttpEntity<>(paymentIntent, headers);
        
        try {
            log.info("Creando Payment Intent en GetNet: {}", paymentIntent.getOrderId());
            
            ResponseEntity<GetnetPaymentIntentResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                GetnetPaymentIntentResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                log.info("Payment Intent creado exitosamente: {}", response.getBody().getPaymentIntentId());
                return response.getBody();
            } else {
                throw new RuntimeException("Error al crear Payment Intent. Status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error al crear Payment Intent en GetNet", e);
            throw new RuntimeException("Error al crear Payment Intent en GetNet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancela un pago en GetNet
     */
    public void cancelPayment(String paymentId) {
        String url = config.getBaseUrl() + "/payments/" + paymentId + "/cancellation";
        
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> request = new HttpEntity<>(headers);
        
        try {
            log.info("Cancelando pago en GetNet: {}", paymentId);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                Void.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                log.info("Pago cancelado exitosamente: {}", paymentId);
            } else {
                throw new RuntimeException("Error al cancelar pago. Status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error al cancelar pago en GetNet: {}", paymentId, e);
            throw new RuntimeException("Error al cancelar pago en GetNet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reembolsa un pago en GetNet
     */
    public GetnetRefundResponse refundPayment(String paymentId, GetnetRefundRequest refundRequest) {
        String url = config.getBaseUrl() + "/payments/" + paymentId + "/refund";
        
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<GetnetRefundRequest> request = new HttpEntity<>(refundRequest, headers);
        
        try {
            log.info("Reembolsando pago en GetNet: {} - Monto: {}", paymentId, refundRequest.getAmount());
            
            ResponseEntity<GetnetRefundResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                GetnetRefundResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.CREATED && response.getBody() != null) {
                log.info("Pago reembolsado exitosamente: {}", paymentId);
                return response.getBody();
            } else {
                throw new RuntimeException("Error al reembolsar pago. Status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error al reembolsar pago en GetNet: {}", paymentId, e);
            throw new RuntimeException("Error al reembolsar pago en GetNet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Crea headers de autenticación con OAuth2
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Obtener token de acceso
        String token = getAccessToken();
        headers.setBearerAuth(token);
        
        // Extraer seller_id del token JWT y usarlo en el header
        String sellerId = extractSellerIdFromToken(token);
        if (sellerId != null) {
            headers.set("x-seller-id", sellerId);
            log.info("Usando seller_id del token: {}", sellerId);
        } else {
            log.warn("No se pudo extraer seller_id del token, usando configurado: {}", config.getSellerId());
            headers.set("x-seller-id", config.getSellerId());
        }
        
        return headers;
    }
    
    /**
     * Obtiene un token de acceso OAuth2 de GetNet
     */
    private String getAccessToken() {
        // Verificar si el token actual es válido
        if (this.accessToken != null && System.currentTimeMillis() < this.tokenExpiry) {
            log.info("Reutilizando token existente. Expira en: {} ms", this.tokenExpiry - System.currentTimeMillis());
            return this.accessToken;
        }
        
        log.info("Token no válido o expirado. accessToken: {}, tokenExpiry: {}, currentTime: {}", 
                this.accessToken != null ? "presente" : "null", 
                this.tokenExpiry, 
                System.currentTimeMillis());
        
        try {
            String url = config.getAuthUrl();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(config.getApiKey(), config.getApiSecret());
            
            String body = "grant_type=client_credentials";
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            
            log.info("Obteniendo token de acceso de GetNet...");
            
            ResponseEntity<GetnetAuthResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                GetnetAuthResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                GetnetAuthResponse authResponse = response.getBody();
                
                // Debug: ver qué está retornando GetNet
                log.info("=== DEBUG AUTH RESPONSE ===");
                log.info("Response completa: {}", authResponse);
                log.info("accessToken: {}", authResponse.getAccessToken());
                log.info("tokenType: {}", authResponse.getTokenType());
                log.info("expiresIn: {}", authResponse.getExpiresIn());
                log.info("scope: {}", authResponse.getScope());
                log.info("==========================");
                
                this.accessToken = authResponse.getAccessToken();
                
                // Manejar expires_in null - usar valor por defecto de 1 hora
                Integer expiresIn = authResponse.getExpiresIn();
                if (expiresIn == null) {
                    expiresIn = 3600; // 1 hora por defecto
                    log.warn("expires_in es null, usando valor por defecto: {} segundos", expiresIn);
                }
                
                // El token expira en 1 hora, pero lo renovamos 10 minutos antes para mayor seguridad
                this.tokenExpiry = System.currentTimeMillis() + (expiresIn - 600) * 1000L;
                
                log.info("Token de acceso obtenido y guardado exitosamente. accessToken: {}, tokenExpiry: {}, expira en: {} segundos, renovación en: {} segundos", 
                        this.accessToken != null ? "presente" : "null",
                        this.tokenExpiry,
                        expiresIn, expiresIn - 600);
                
                // Verificar que el token se guardó correctamente antes de retornarlo
                if (this.accessToken == null) {
                    log.error("ERROR: El token no se guardó correctamente");
                    throw new RuntimeException("Error interno: El token no se guardó correctamente");
                }
                
                return this.accessToken;
            } else {
                throw new RuntimeException("Error al obtener token de acceso. Status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error al obtener token de acceso de GetNet", e);
            throw new RuntimeException("Error al obtener token de acceso de GetNet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Modelo interno para la respuesta de autenticación
     */
    private static class GetnetAuthResponse {
        @com.fasterxml.jackson.annotation.JsonProperty("access_token")
        private String accessToken;
        
        @com.fasterxml.jackson.annotation.JsonProperty("token_type")
        private String tokenType;
        
        @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
        private Integer expiresIn;
        
        private String scope;
        
        // Getters y setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public Integer getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Integer expiresIn) { this.expiresIn = expiresIn; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
    }

    /**
     * Método público para probar la autenticación
     */
    public String testAuth() {
        return getAccessToken();
    }
    
    /**
     * Fuerza la renovación del token (útil para debugging)
     */
    public String forceTokenRefresh() {
        log.info("Forzando renovación del token...");
        this.accessToken = null;
        this.tokenExpiry = 0;
        return getAccessToken();
    }
    
    /**
     * Obtiene información del estado del token (útil para debugging)
     */
    public String getTokenStatus() {
        if (accessToken == null) {
            return "No hay token";
        }
        long timeUntilExpiry = tokenExpiry - System.currentTimeMillis();
        if (timeUntilExpiry <= 0) {
            return "Token expirado";
        }
        return String.format("Token válido, expira en %d segundos", timeUntilExpiry / 1000);
    }
    
    /**
     * Debug del estado interno del token
     */
    public String debugTokenState() {
        return String.format("accessToken: %s, tokenExpiry: %d, currentTime: %d, timeUntilExpiry: %d", 
                accessToken != null ? "presente" : "null",
                tokenExpiry,
                System.currentTimeMillis(),
                tokenExpiry - System.currentTimeMillis());
    }
    
    /**
     * Extrae el seller_id del token JWT
     */
    private String extractSellerIdFromToken(String token) {
        try {
            // Decodificar el payload del JWT (segunda parte)
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                log.warn("Token JWT inválido, no se puede extraer seller_id");
                return null;
            }
            
            String payload = parts[1];
            // Agregar padding si es necesario
            while (payload.length() % 4 != 0) {
                payload += "=";
            }
            
            // Decodificar base64
            String decodedPayload = new String(Base64.getDecoder().decode(payload));
            log.info("=== TOKEN JWT DECODIFICADO ===");
            log.info("Payload completo: {}", decodedPayload);
            
            // Buscar sellerId en el payload (formato simple)
            if (decodedPayload.contains("\"sellerId\":")) {
                int start = decodedPayload.indexOf("\"sellerId\":\"") + 12;
                int end = decodedPayload.indexOf("\"", start);
                if (start > 11 && end > start) {
                    String sellerId = decodedPayload.substring(start, end);
                    log.info("✅ Seller ID extraído del token: {}", sellerId);
                    return sellerId;
                }
            }
            
            // Buscar subname (alternativo)
            if (decodedPayload.contains("\"subname\":")) {
                int start = decodedPayload.indexOf("\"subname\":\"") + 11;
                int end = decodedPayload.indexOf("\"", start);
                if (start > 10 && end > start) {
                    String subname = decodedPayload.substring(start, end);
                    log.info("✅ Subname extraído del token: {}", subname);
                    return subname;
                }
            }
            
            log.warn("❌ No se encontró sellerId ni subname en el token JWT");
            log.info("================================");
            return null;
            
        } catch (Exception e) {
            log.error("Error al extraer seller_id del token JWT", e);
            return null;
        }
    }
}
