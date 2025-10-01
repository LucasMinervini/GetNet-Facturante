package com.gf.connector.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio simple de rate limiting para proteger contra ataques de fuerza bruta
 * En producción, se recomienda usar Redis con Bucket4j para escalabilidad
 */
@Slf4j
@Service
public class RateLimitService {

    @Value("${security.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;
    
    @Value("${security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    // Cache simple en memoria (en producción usar Redis)
    private final ConcurrentHashMap<String, RateLimitEntry> rateLimitCache = new ConcurrentHashMap<>();
    
    private static class RateLimitEntry {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private LocalDateTime windowStart;
        
        public RateLimitEntry() {
            this.windowStart = LocalDateTime.now();
        }
        
        public boolean isAllowed(int maxRequests) {
            LocalDateTime now = LocalDateTime.now();
            
            // Si han pasado más de 1 minuto, resetear contador
            if (windowStart.isBefore(now.minusMinutes(1))) {
                requestCount.set(0);
                windowStart = now;
            }
            
            return requestCount.incrementAndGet() <= maxRequests;
        }
        
        public int getRemainingRequests(int maxRequests) {
            return Math.max(0, maxRequests - requestCount.get());
        }
    }

    /**
     * Verifica si una IP puede hacer una petición
     * @param clientIp IP del cliente
     * @return true si está permitido, false si excede el límite
     */
    public boolean isAllowed(String clientIp) {
        if (!rateLimitEnabled) {
            return true;
        }
        
        try {
            RateLimitEntry entry = rateLimitCache.computeIfAbsent(clientIp, k -> new RateLimitEntry());
            boolean allowed = entry.isAllowed(requestsPerMinute);
            
            if (!allowed) {
                log.warn("Rate limit excedido para IP: {} ({} requests/min)", clientIp, requestsPerMinute);
            }
            
            return allowed;
        } catch (Exception e) {
            log.error("Error en rate limiting para IP {}: {}", clientIp, e.getMessage());
            // En caso de error, permitir la petición
            return true;
        }
    }
    
    /**
     * Obtiene el número de peticiones restantes para una IP
     * @param clientIp IP del cliente
     * @return Número de peticiones restantes
     */
    public int getRemainingRequests(String clientIp) {
        if (!rateLimitEnabled) {
            return Integer.MAX_VALUE;
        }
        
        RateLimitEntry entry = rateLimitCache.get(clientIp);
        if (entry == null) {
            return requestsPerMinute;
        }
        
        return entry.getRemainingRequests(requestsPerMinute);
    }
    
    /**
     * Limpia entradas expiradas del cache
     */
    public void cleanupExpiredEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(2);
        rateLimitCache.entrySet().removeIf(entry -> 
            entry.getValue().windowStart.isBefore(cutoff)
        );
    }
}
