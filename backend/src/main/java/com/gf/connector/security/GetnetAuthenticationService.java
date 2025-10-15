package com.gf.connector.security;

import com.gf.connector.domain.BillingSettings;
import com.gf.connector.repo.BillingSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para manejar la autenticación con APIs de Getnet usando Bearer JWT
 * según los estándares de Getnet para llamadas API salientes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetnetAuthenticationService {

    private final BillingSettingsRepository billingSettingsRepository;
    private final RestTemplate restTemplate;
    
    @Value("${getnet.environment:sandbox}")
    private String environment;
    
    @Value("${getnet.api-key:}")
    private String apiKey;
    
    @Value("${getnet.api-secret:}")
    private String apiSecret;
    
    @Value("${getnet.seller-id:}")
    private String sellerId;
    
    @Value("${getnet.oauth.sandbox}")
    private String oauthUrlSandbox;
    
    @Value("${getnet.oauth.homologacao}")
    private String oauthUrlHomologacao;
    
    @Value("${getnet.oauth.production}")
    private String oauthUrlProduction;
    
    @Value("${getnet.api.sandbox}")
    private String apiUrlSandbox;
    
    @Value("${getnet.api.homologacao}")
    private String apiUrlHomologacao;
    
    @Value("${getnet.api.production}")
    private String apiUrlProduction;
    
    // Cache simple de tokens en memoria (en producción considerar Redis)
    private final ConcurrentHashMap<String, TokenCacheEntry> tokenCache = new ConcurrentHashMap<>();
    
    private static class TokenCacheEntry {
        String accessToken;
        Instant expiresAt;
        
        TokenCacheEntry(String accessToken, long expiresInSeconds) {
            this.accessToken = accessToken;
            this.expiresAt = Instant.now().plusSeconds(expiresInSeconds - 60); // 60s de margen
        }
        
        boolean isValid() {
            return Instant.now().isBefore(expiresAt);
        }
    }

    /**
     * Obtiene un token de acceso de Getnet para realizar llamadas API autenticadas
     * Con cache para evitar llamadas innecesarias
     * @param tenantId ID del tenant para obtener credenciales específicas
     * @return Token JWT de Getnet o null si falla
     */
    public String getGetnetAccessToken(UUID tenantId) {
        try {
            String cacheKey = tenantId != null ? tenantId.toString() : "global";
            
            // Verificar cache
            TokenCacheEntry cached = tokenCache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                log.debug("Usando token de Getnet en cache para: {}", cacheKey);
                return cached.accessToken;
            }
            
            // Obtener configuración del tenant
            Optional<BillingSettings> settingsOpt = tenantId != null 
                ? billingSettingsRepository.findByTenantIdAndActivoTrue(tenantId)
                : Optional.empty();
            
            if (settingsOpt.isEmpty() && tenantId != null) {
                log.warn("No se encontró configuración de billing para tenant: {}", tenantId);
            }
            
            // Construir URL de autenticación según el entorno
            String authUrl = getOAuthUrl();
            
            // Preparar headers para OAuth
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + buildBasicAuth(apiKey, apiSecret));
            
            // Preparar body de autenticación según documentación Getnet
            String authBody = "scope=oob&grant_type=client_credentials";
            
            HttpEntity<String> request = new HttpEntity<>(authBody, headers);
            
            // Realizar llamada de autenticación
            log.info("Solicitando token de acceso a Getnet ({}): {}", environment, authUrl);
            
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                authUrl,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                String accessToken = (String) responseBody.get("access_token");
                Integer expiresIn = (Integer) responseBody.getOrDefault("expires_in", 3600);
                
                if (accessToken != null) {
                    // Cachear token
                    tokenCache.put(cacheKey, new TokenCacheEntry(accessToken, expiresIn));
                    log.info("Token de Getnet obtenido exitosamente (expira en {} segundos)", expiresIn);
                    return accessToken;
                }
            }
            
            log.error("Respuesta inválida de Getnet OAuth: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error al obtener token de Getnet para tenant {}: {}", tenantId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Realiza una llamada autenticada a la API de Getnet
     * @param endpoint Endpoint de la API
     * @param method Método HTTP
     * @param body Cuerpo de la petición
     * @param tenantId ID del tenant
     * @return Respuesta de la API
     */
    public <T> T makeAuthenticatedCall(String endpoint, String method, Object body, Class<T> responseType, UUID tenantId) {
        try {
            String token = getGetnetAccessToken(tenantId);
            if (token == null) {
                throw new RuntimeException("No se pudo obtener token de autenticación");
            }
            
            // Preparar headers con Bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            
            String baseUrl = getApiUrl();
            String url = baseUrl + endpoint;

            HttpEntity<?> requestEntity;
            if ("GET".equalsIgnoreCase(method) && body instanceof Map<?, ?> params && !params.isEmpty()) {
                // Adjuntar query params
                StringBuilder urlBuilder = new StringBuilder(url);
                urlBuilder.append("?");
                ((Map<?, ?>) params).forEach((k, v) -> {
                    if (v != null) {
                        urlBuilder.append(k).append("=").append(java.net.URLEncoder.encode(String.valueOf(v), java.nio.charset.StandardCharsets.UTF_8)).append("&");
                    }
                });
                url = urlBuilder.substring(0, urlBuilder.length() - 1);
                requestEntity = new HttpEntity<>(headers);
            } else {
                requestEntity = new HttpEntity<>(body, headers);
            }

            log.info("Realizando llamada autenticada a Getnet: {} {}", method, url);

            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

            ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestEntity, responseType);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

            throw new RuntimeException("Llamada a Getnet no exitosa: " + response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Error en llamada autenticada a Getnet: {}", e.getMessage(), e);
            throw new RuntimeException("Error en llamada a Getnet", e);
        }
    }
    
    /**
     * Obtiene reportes de transacciones de Getnet usando Merchant Reporting
     * @param tenantId ID del tenant
     * @param startDate Fecha de inicio
     * @param endDate Fecha de fin
     * @return Lista de transacciones
     */
    public Map<String, Object> getMerchantReport(UUID tenantId, String startDate, String endDate) {
        try {
            String endpoint = "/v1/reports/transactions";
            Map<String, Object> params = Map.of(
                "start_date", startDate,
                "end_date", endDate,
                "status", "PAID"
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = makeAuthenticatedCall(endpoint, "GET", params, Map.class, tenantId);
            return response;
            
        } catch (Exception e) {
            log.error("Error al obtener reporte de Getnet: {}", e.getMessage(), e);
            return Map.of();
        }
    }
    
    private String getOAuthUrl() {
        return switch (environment.toLowerCase()) {
            case "production" -> oauthUrlProduction;
            case "homologacao", "pre" -> oauthUrlHomologacao;
            default -> oauthUrlSandbox;
        };
    }
    
    private String getApiUrl() {
        return switch (environment.toLowerCase()) {
            case "production" -> apiUrlProduction;
            case "homologacao", "pre" -> apiUrlHomologacao;
            default -> apiUrlSandbox;
        };
    }
    
    private String buildBasicAuth(String apiKey, String apiSecret) {
        String credentials = apiKey + ":" + apiSecret;
        return java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }
    
    /**
     * Limpia el cache de tokens (útil para rotación de credenciales)
     */
    public void clearTokenCache() {
        tokenCache.clear();
        log.info("Cache de tokens de Getnet limpiado");
    }
}
