package com.gf.connector;

import com.gf.connector.service.BillingSettingsService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ConnectorBackendApplication {
  public static void main(String[] args) {
    // Cargar variables de entorno desde .env
    Dotenv dotenv = Dotenv.configure()
        .directory("./")
        .ignoreIfMalformed()
        .ignoreIfMissing()
        .load();
    
    // Establecer las variables en el sistema
    dotenv.entries().forEach(entry -> {
      System.setProperty(entry.getKey(), entry.getValue());
    });
    
    SpringApplication.run(ConnectorBackendApplication.class, args);
  }

  @Bean
  public CommandLineRunner initBillingSettings(BillingSettingsService billingSettingsService) {
    return args -> {
      // Crear configuración por defecto si no existe
      billingSettingsService.createDefaultSettingsIfNotExists();
    };
  }
}
