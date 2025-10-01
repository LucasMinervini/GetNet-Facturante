package com.gf.connector;

import com.gf.connector.service.BillingSettingsService;
import com.gf.connector.service.UserService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableScheduling
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
  public CommandLineRunner initBillingSettings(BillingSettingsService billingSettingsService, UserService userService) {
    return args -> {
      // Multi-tenant: tomar el tenantId del admin inicial si existe; sino crear admin y usar su tenantId
      var adminOpt = userService.findByUsername("admin");
      java.util.UUID tenantId;
      if (adminOpt.isPresent()) {
        tenantId = adminOpt.get().getTenantId();
      } else {
        // Crear admin inicial y usar su tenantId
        try {
          var admin = userService.createUser("admin", "admin", "admin@getnet-facturante.com", "Admin", "User");
          tenantId = admin.getTenantId();
          System.out.println("Usuario admin creado: admin/admin");
        } catch (Exception e) {
          // Si falla por duplicado de email/usuario, obtener nuevamente
          tenantId = userService.findByUsername("admin").map(u -> u.getTenantId()).orElseGet(() -> java.util.UUID.randomUUID());
        }
      }
      // Crear configuración por defecto si no existe para ese tenant
      billingSettingsService.createDefaultSettingsIfNotExists(tenantId);
    };
  }
}
