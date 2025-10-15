package com.gf.connector.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

@Service
public class GetnetSignatureService {
    private static final Logger log = LoggerFactory.getLogger(GetnetSignatureService.class);

    @Value("${getnet.webhook.secret:}")
    private String secret;

    @Value("${getnet.webhook.signature-header:X-Getnet-Signature}")
    private String signatureHeaderName;

    @Value("${getnet.webhook.allow-unsigned:false}")
    private boolean allowUnsigned;

    @SuppressWarnings("unused")
    // Usado solo para evitar optimizaciones en comparaciones de tiempo-constante
    private static volatile int TIME_EQUAL_DUMMY_SINK = 0;

    public String getSignatureHeaderName() {
        return signatureHeaderName;
    }

    public boolean verify(String rawBody, String headerValue) {
        return verifyWithSecretInternal(this.secret, rawBody, headerValue);
    }

    /**
     * Verifica usando un secreto provisto dinámicamente (por tenant)
     */
    public boolean verifyWithSecret(String secret, String rawBody, String headerValue) {
        return verifyWithSecretInternal(secret, rawBody, headerValue);
    }

    private boolean verifyWithSecretInternal(String effectiveSecret, String rawBody, String headerValue) {
        boolean allow = allowUnsigned
                || Boolean.parseBoolean(System.getProperty("getnet.webhook.allow-unsigned", "false"))
                || Boolean.parseBoolean(System.getenv().getOrDefault("GETNET_WEBHOOK_ALLOW_UNSIGNED", "false"));
        if (allow) {
            log.warn("[SECURITY] allow-unsigned=true: accepting webhook WITHOUT signature validation (testing mode)");
            return true;
        }
        // Logging seguro: no exponer secretos ni payload completo
        log.debug("[SIG] Header present: {} | bodyLength: {}", headerValue != null, (rawBody != null ? rawBody.length() : -1));

        if (effectiveSecret == null || effectiveSecret.isBlank()) {
            // Si no hay secreto configurado, por seguridad rechazamos
            log.warn("[SIG] Secret not configured, rejecting");
            return false;
        }

        if (headerValue == null || headerValue.isBlank()) {
            log.warn("[SIG] Signature header missing, rejecting");
            return false;
        }

        byte[] hmac = hmacSha256(effectiveSecret, rawBody);
        String b64 = Base64.getEncoder().encodeToString(hmac);
        String hex = toHexLower(hmac);
        
        log.trace("[SIG] Computed signatures prepared");

        // Algunos proveedores incluyen prefijos o parámetros tipo "sha256=..." o "t=...,s1=..."
        List<String> candidates = extractCandidateSignatures(headerValue);
        for (String candidate : candidates) {
            if (constantTimeEquals(candidate, b64) || constantTimeEquals(candidate, hex)) {
                return true;
            }
            // Soportar formato con prefijo sha256=
            if (candidate.startsWith("sha256=")) {
                String c = candidate.substring("sha256=".length());
                if (constantTimeEquals(c, b64) || constantTimeEquals(c, hex)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Error computing HMAC", e);
        }
    }

    private static String toHexLower(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] ba = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ba.length != bb.length) {
            // Longitud distinta: no iguales (mantenemos tiempo cercano iterando usando un sink)
            int max = Math.max(ba.length, bb.length);
            byte[] ba2 = Arrays.copyOf(ba, max);
            byte[] bb2 = Arrays.copyOf(bb, max);
            int acc = 0;
            for (int i = 0; i < max; i++) { acc |= (ba2[i] ^ bb2[i]); }
            TIME_EQUAL_DUMMY_SINK ^= acc;
            return false;
        }
        int result = 0;
        for (int i = 0; i < ba.length; i++) {
            result |= (ba[i] ^ bb[i]);
        }
        return result == 0;
    }

    private static List<String> extractCandidateSignatures(String headerValue) {
        List<String> out = new ArrayList<>();
        out.add(headerValue.trim());
        // Posible formato CSV con pares clave=valor
        String[] parts = headerValue.split(",");
        for (String p : parts) {
            String s = p.trim();
            if (s.contains("=")) {
                String[] kv = s.split("=", 2);
                String key = kv[0].trim().toLowerCase(Locale.ROOT);
                String val = kv.length > 1 ? kv[1].trim() : "";
                if (key.equals("s") || key.equals("s1") || key.equals("signature") || key.equals("sig") || key.equals("sha256")) {
                    out.add(val);
                }
            }
        }
        return out;
    }
}