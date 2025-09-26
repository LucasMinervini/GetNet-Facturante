package com.gf.connector.facturante.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "facturante")
public class FacturanteConfig {
    private String serviceUrl;
    private String empresa;
    private String usuario;
    private String password;
    private String prefijo = "0001";
    private String tipoComprobante = "FB"; // Factura B por defecto
}