package com.gf.connector.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class GetnetSignatureServiceTest {

    private GetnetSignatureService service;

    @BeforeEach
    void setup() {
        service = new GetnetSignatureService();
        ReflectionTestUtils.setField(service, "allowUnsigned", false);
        ReflectionTestUtils.setField(service, "secret", "super-secret");
        ReflectionTestUtils.setField(service, "signatureHeaderName", "X-Getnet-Signature");
    }

    @Test
    void verify_validSignature_b64_and_hex_passes() throws Exception {
        String raw = "{\"a\":1}";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec("super-secret".getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hmac = mac.doFinal(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String b64 = java.util.Base64.getEncoder().encodeToString(hmac);
        String hex = java.util.stream.IntStream.range(0, hmac.length)
                .mapToObj(i -> String.format("%02x", hmac[i]))
                .reduce("", String::concat);

        assertThat(service.verify(raw, b64)).isTrue();
        assertThat(service.verify(raw, hex)).isTrue();
        assertThat(service.verify(raw, "sha256=" + b64)).isTrue();
    }

    @Test
    void verify_missingOrWrongSignature_rejected_unlessAllowUnsigned() {
        String raw = "{}";
        assertThat(service.verify(raw, null)).isFalse();
        assertThat(service.verify(raw, "bad")).isFalse();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "allowUnsigned", true);
        assertThat(service.verify(raw, null)).isTrue();
    }
}


