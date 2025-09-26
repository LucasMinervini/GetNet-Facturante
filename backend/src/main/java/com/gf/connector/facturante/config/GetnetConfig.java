package com.gf.connector.facturante.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "getnet")
public class GetnetConfig {
    
    private String apiKey;
    private String apiSecret;
    private String sellerId;
    private String environment = "production"; // sandbox, pre, production
    
    // URLs según el ambiente
    private String baseUrl;
    private String authUrl;
    
    public String getBaseUrl() {
        if (baseUrl != null) {
            System.out.println("🔍 GetnetConfig: Usando baseUrl personalizado: " + baseUrl);
            return baseUrl;
        }
        
        String url;
        switch (environment.toLowerCase()) {
            case "production":
                url = "https://api.globalgetnet.com/digital-checkout/v1";
                break;
            case "pre":
                url = "https://api.pre.globalgetnet.com/digital-checkout/v1";
                break;
            case "sandbox":
            default:
                url = "https://api-sbx.pre.globalgetnet.com/digital-checkout/v1";
                break;
        }
        
        System.out.println("🔍 GetnetConfig: Environment: " + environment + ", Base URL: " + url);
        return url;
    }
    
    public String getAuthUrl() {
        if (authUrl != null) {
            System.out.println("🔍 GetnetConfig: Usando authUrl personalizado: " + authUrl);
            return authUrl;
        }
        
        String url;
        switch (environment.toLowerCase()) {
            case "production":
                url = "https://api.globalgetnet.com/authentication/oauth2/access_token";
                break;
            case "pre":
                url = "https://api.pre.globalgetnet.com/authentication/oauth2/access_token";
                break;
            case "sandbox":
            default:
                url = "https://api-sbx.pre.globalgetnet.com/authentication/oauth2/access_token";
                break;
        }
        
        System.out.println("🔍 GetnetConfig: Environment: " + environment + ", Auth URL: " + url);
        return url;
    }
    
    public boolean isProduction() {
        return "production".equalsIgnoreCase(environment);
    }
    
    public boolean isSandbox() {
        return "sandbox".equalsIgnoreCase(environment);
    }
}
