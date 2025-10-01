package com.gf.connector.controllers;

import com.gf.connector.dto.BillingSettingsDto;
import com.gf.connector.service.BillingSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/billing-settings")
@RequiredArgsConstructor
@Tag(name = "Configuración de Facturación", description = "API para gestionar la configuración de facturación")
public class BillingSettingsController {
    
    private final BillingSettingsService billingSettingsService;
    // Compatibilidad con tests existentes
    private static final java.util.UUID DEFAULT_TEST_TENANT = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    
    @GetMapping("/active")
    @Operation(summary = "Obtener configuración activa", description = "Obtiene la configuración de facturación actualmente activa")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración encontrada"),
        @ApiResponse(responseCode = "404", description = "No hay configuración activa")
    })
    public ResponseEntity<BillingSettingsDto> getActiveSettings(@RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        if (tenantId == null) {
            return billingSettingsService.getActiveSettingsDto()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return billingSettingsService.getActiveSettingsDto(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Overloads para tests unitarios (sin tenant explícito)
    public ResponseEntity<BillingSettingsDto> getActiveSettings() {
        return billingSettingsService.getActiveSettingsDto()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "Listar todas las configuraciones", description = "Obtiene todas las configuraciones de facturación")
    @ApiResponse(responseCode = "200", description = "Lista de configuraciones obtenida")
    public ResponseEntity<List<BillingSettingsDto>> getAllSettings(@RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        if (tenantId == null) {
            List<BillingSettingsDto> settings = billingSettingsService.getAllSettings();
            return ResponseEntity.ok(settings);
        }
        List<BillingSettingsDto> settings = billingSettingsService.getAllSettings(tenantId);
        return ResponseEntity.ok(settings);
    }

    // Overload para tests
    public ResponseEntity<List<BillingSettingsDto>> getAllSettings() {
        List<BillingSettingsDto> settings = billingSettingsService.getAllSettings();
        return ResponseEntity.ok(settings);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Obtener configuración por ID", description = "Obtiene una configuración específica por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración encontrada"),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    public ResponseEntity<BillingSettingsDto> getSettingsById(
            @Parameter(description = "ID de la configuración", required = true) 
            @PathVariable UUID id,
            @RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        if (tenantId == null) {
            return billingSettingsService.getSettingsById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        return billingSettingsService.getSettingsById(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Overload para tests
    public ResponseEntity<BillingSettingsDto> getSettingsById(UUID id) {
        return billingSettingsService.getSettingsById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Crear nueva configuración", description = "Crea una nueva configuración de facturación")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponse(responseCode = "200", description = "Configuración creada exitosamente")
    public ResponseEntity<BillingSettingsDto> createSettings(
            @Parameter(description = "Datos de la configuración", required = true)
            @RequestBody BillingSettingsDto dto,
            @RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        try {
            if (tenantId == null) {
                BillingSettingsDto created = billingSettingsService.createSettings(dto);
                return ResponseEntity.ok(created);
            }
            BillingSettingsDto created = billingSettingsService.createSettings(dto, tenantId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error al crear configuración", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Overload para tests
    public ResponseEntity<BillingSettingsDto> createSettings(BillingSettingsDto dto) {
        try {
            BillingSettingsDto created = billingSettingsService.createSettings(dto);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error al crear configuración", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar configuración", description = "Actualiza una configuración existente")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración actualizada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    public ResponseEntity<BillingSettingsDto> updateSettings(
            @Parameter(description = "ID de la configuración", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Datos actualizados de la configuración", required = true)
            @RequestBody BillingSettingsDto dto,
            @RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        try {
            if (tenantId == null) {
                BillingSettingsDto updated = billingSettingsService.updateSettings(id, dto);
                return ResponseEntity.ok(updated);
            }
            BillingSettingsDto updated = billingSettingsService.updateSettings(id, dto, tenantId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Configuración no encontrada: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al actualizar configuración", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Overload para tests
    public ResponseEntity<BillingSettingsDto> updateSettings(UUID id, BillingSettingsDto dto) {
        try {
            BillingSettingsDto updated = billingSettingsService.updateSettings(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Configuración no encontrada: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al actualizar configuración", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activar configuración", description = "Activa una configuración específica y desactiva las demás")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración activada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada")
    })
    public ResponseEntity<BillingSettingsDto> activateSettings(
            @Parameter(description = "ID de la configuración a activar", required = true)
            @PathVariable UUID id,
            @RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        try {
            if (tenantId == null) {
                BillingSettingsDto activated = billingSettingsService.activateSettings(id);
                return ResponseEntity.ok(activated);
            }
            BillingSettingsDto activated = billingSettingsService.activateSettings(id, tenantId);
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            log.error("Configuración no encontrada: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al activar configuración", e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Overload para tests
    public ResponseEntity<BillingSettingsDto> activateSettings(UUID id) {
        try {
            BillingSettingsDto activated = billingSettingsService.activateSettings(id);
            return ResponseEntity.ok(activated);
        } catch (IllegalArgumentException e) {
            log.error("Configuración no encontrada: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al activar configuración", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar configuración", description = "Elimina una configuración específica")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuración eliminada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Configuración no encontrada"),
        @ApiResponse(responseCode = "400", description = "No se puede eliminar la única configuración activa")
    })
    public ResponseEntity<Void> deleteSettings(
            @Parameter(description = "ID de la configuración a eliminar", required = true)
            @PathVariable UUID id,
            @RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        try {
            if (tenantId == null) {
                billingSettingsService.deleteSettings(id);
                return ResponseEntity.ok().build();
            }
            billingSettingsService.deleteSettings(id, tenantId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Configuración no encontrada: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("No se puede eliminar la configuración: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error al eliminar configuración", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Overload para tests
    public ResponseEntity<Void> deleteSettings(UUID id) {
        try {
            billingSettingsService.deleteSettings(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("Configuración no encontrada: {}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("No se puede eliminar la configuración: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error al eliminar configuración", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/init-default")
    @Operation(summary = "Inicializar configuración por defecto", description = "Crea una configuración por defecto si no existe ninguna")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiResponse(responseCode = "200", description = "Configuración por defecto creada o ya existía")
    public ResponseEntity<BillingSettingsDto> initDefaultSettings(@RequestAttribute(name = "tenantId", required = false) java.util.UUID tenantId) {
        if (tenantId == null) {
            billingSettingsService.createDefaultSettingsIfNotExists();
            return billingSettingsService.getActiveSettingsDto()
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        billingSettingsService.createDefaultSettingsIfNotExists(tenantId);
        return billingSettingsService.getActiveSettingsDto(tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Overload para tests
    public ResponseEntity<BillingSettingsDto> initDefaultSettings() {
        billingSettingsService.createDefaultSettingsIfNotExists();
        return billingSettingsService.getActiveSettingsDto()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
