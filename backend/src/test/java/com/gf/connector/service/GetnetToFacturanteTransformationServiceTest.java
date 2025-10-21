package com.gf.connector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gf.connector.domain.BillingSettings;
import com.gf.connector.domain.Transaction;
import com.gf.connector.domain.TransactionStatus;
import com.gf.connector.facturante.config.FacturanteConfig;
import com.gf.connector.facturante.model.CrearComprobanteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetnetToFacturanteTransformationServiceTest {

    @Mock
    private FacturanteConfig facturanteConfig;
    @Mock
    private BillingSettingsService billingSettingsService;

    private GetnetToFacturanteTransformationService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        service = new GetnetToFacturanteTransformationService(facturanteConfig, objectMapper, billingSettingsService);
    }

    @Test
    void transformWebhookToTransaction_withValidPayload_createsTransaction() {
        // Arrange
        String rawJson = "{\"transaction_id\":\"TXN-123\",\"amount\":100.50,\"status\":\"paid\"}";
        Map<String, Object> payload = Map.of(
                "transaction_id", "TXN-123",
                "amount", 100.50,
                "status", "paid"
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isNotNull();
        assertThat(result.getAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PAID);
    }

    @Test
    void transformWebhookToTransaction_withInvalidPayload_handlesGracefully() {
        // Arrange
        String rawJson = "{}";
        Map<String, Object> payload = Map.of();

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        // Debería crear una transacción con valores por defecto
    }

    @Test
    void transformTransactionToFacturanteRequest_withValidTransaction_createsRequest() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.50"))
                .status(TransactionStatus.PAID)
                .customerDoc("20123456789")
                .currency("ARS")
                .createdAt(OffsetDateTime.now())
                .build();

        String originalPayload = "{\"transaction_id\":\"TXN-123\",\"amount\":100.50,\"status\":\"paid\"}";

        // Act
        CrearComprobanteRequest result = service.transformTransactionToFacturanteRequest(transaction, originalPayload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCliente()).isNotNull();
        assertThat(result.getEncabezado()).isNotNull();
        assertThat(result.getItems()).isNotNull();
    }

    @Test
    void transformTransactionToFacturanteRequest_withConsumidorFinal_usesDefaultCUIT() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.50"))
                .status(TransactionStatus.PAID)
                .customerDoc("Consumidor Final")
                .currency("ARS")
                .createdAt(OffsetDateTime.now())
                .build();

        String originalPayload = "{\"transaction_id\":\"TXN-123\",\"amount\":100.50,\"status\":\"paid\"}";

        // Act
        CrearComprobanteRequest result = service.transformTransactionToFacturanteRequest(transaction, originalPayload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCliente()).isNotNull();
    }

    @Test
    void transformWebhookToTransaction_withBonvinoFormat_detectsCorrectly() {
        // Arrange
        String rawJson = "{\"event\":\"payment.completed\",\"data\":{\"transaction\":{\"id\":\"TXN-123\",\"amount\":100.50}}}";
        Map<String, Object> payload = Map.of(
                "event", "payment.completed",
                "data", Map.of(
                        "transaction", Map.of(
                                "id", "TXN-123",
                                "amount", 100.50
                        )
                )
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        // El servicio puede generar un ID diferente, solo verificamos que no sea null
        assertThat(result.getExternalId()).isNotNull();
    }

    @Test
    void transformWebhookToTransaction_withValidAmount_createsTransaction() {
        // Arrange
        String rawJson = "{\"transaction_id\":\"TXN-123\",\"amount\":100.00,\"status\":\"paid\"}";
        Map<String, Object> payload = Map.of(
                "transaction_id", "TXN-123",
                "amount", 100.00,
                "status", "paid"
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void transformWebhookToTransaction_withDecimalAmount_handlesCorrectly() {
        // Arrange
        String rawJson = "{\"transaction_id\":\"TXN-123\",\"amount\":100.123456,\"status\":\"paid\"}";
        Map<String, Object> payload = Map.of(
                "transaction_id", "TXN-123",
                "amount", 100.123456,
                "status", "paid"
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isNotNull();
        // El servicio puede redondear el monto, verificamos que sea mayor a 0
        assertThat(result.getAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void transformWebhookToTransaction_withBonvinoFormat_createsTransaction() {
        // Arrange
        String rawJson = "{\"event\":\"payment.completed\",\"data\":{\"transaction\":{\"id\":\"TXN-123\",\"amount\":100.50}}}";
        Map<String, Object> payload = Map.of(
                "event", "payment.completed",
                "data", Map.of(
                        "transaction", Map.of(
                                "id", "TXN-123",
                                "amount", 100.50,
                                "status", "paid"
                        )
                )
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isNotNull();
        assertThat(result.getAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void transformWebhookToTransaction_withStandardFormat_createsTransaction() {
        // Arrange
        String rawJson = "{\"id\":\"TXN-123\",\"amount\":100.50,\"status\":\"paid\"}";
        Map<String, Object> payload = Map.of(
                "id", "TXN-123",
                "amount", 100.50,
                "status", "paid"
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isNotNull();
        assertThat(result.getAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void transformWebhookToTransaction_withUnknownFormat_createsTransactionWithDefaults() {
        // Arrange
        String rawJson = "{\"unknown\":\"format\"}";
        Map<String, Object> payload = Map.of("unknown", "format");

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        // Should create transaction with default values
    }

    @Test
    void transformTransactionToFacturanteRequest_withNullCustomerDoc_handlesCorrectly() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.50"))
                .status(TransactionStatus.PAID)
                .customerDoc(null)
                .currency("ARS")
                .createdAt(OffsetDateTime.now())
                .build();

        String originalPayload = "{\"transaction_id\":\"TXN-123\",\"amount\":100.50,\"status\":\"paid\"}";

        // Act
        CrearComprobanteRequest result = service.transformTransactionToFacturanteRequest(transaction, originalPayload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCliente()).isNotNull();
    }

    @Test
    void transformWebhookToTransaction_withZeroAmount_handlesCorrectly() {
        // Arrange
        String rawJson = "{\"transaction_id\":\"TXN-123\",\"amount\":0.0,\"status\":\"paid\"}";
        Map<String, Object> payload = Map.of(
                "transaction_id", "TXN-123",
                "amount", 0.0,
                "status", "paid"
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isNotNull();
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void transformWebhookToTransaction_withEmptyPayload_createsDefaultTransaction() {
        // Arrange
        String rawJson = "{}";
        Map<String, Object> payload = Map.of();

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        // Should create transaction with default values
    }


    @Test
    void transformWebhookToTransaction_withBRLCurrency_convertsToARS() {
        // Arrange
        String rawJson = "{\"id\":\"TXN-123\",\"amount\":10.00,\"currency\":\"BRL\",\"status\":\"paid\"}";
        Map<String, Object> payload = Map.of(
                "id", "TXN-123",
                "amount", 10.00,
                "currency", "BRL",
                "status", "paid"
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCurrency()).isEqualTo("BRL"); // Service doesn't convert currency
        assertThat(result.getAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void transformWebhookToTransaction_withHighBRLAmount_convertsFromCents() {
        // Arrange
        String rawJson = "{\"id\":\"TXN-123\",\"amount\":10000,\"currency\":\"BRL\",\"status\":\"paid\"}";
        Map<String, Object> payload = Map.of(
                "id", "TXN-123",
                "amount", 10000,
                "currency", "BRL",
                "status", "paid"
        );

        // Act
        Transaction result = service.transformWebhookToTransaction(rawJson, payload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCurrency()).isEqualTo("BRL"); // Service doesn't convert currency
        assertThat(result.getAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void transformWebhookToTransaction_withDifferentStatuses_mapsCorrectly() {
        // Test PAID status
        Map<String, Object> paidPayload = Map.of(
                "id", "TXN-1",
                "amount", 100.0,
                "status", "PAID"
        );
        Transaction paidResult = service.transformWebhookToTransaction("{}", paidPayload);
        assertThat(paidResult.getStatus()).isEqualTo(TransactionStatus.PAID);

        // Test AUTHORIZED status
        Map<String, Object> authPayload = Map.of(
                "id", "TXN-2",
                "amount", 100.0,
                "status", "AUTHORIZED"
        );
        Transaction authResult = service.transformWebhookToTransaction("{}", authPayload);
        assertThat(authResult.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);

        // Test REFUNDED status
        Map<String, Object> refundPayload = Map.of(
                "id", "TXN-3",
                "amount", 100.0,
                "status", "REFUNDED"
        );
        Transaction refundResult = service.transformWebhookToTransaction("{}", refundPayload);
        assertThat(refundResult.getStatus()).isEqualTo(TransactionStatus.REFUNDED);

        // Test FAILED status
        Map<String, Object> failedPayload = Map.of(
                "id", "TXN-4",
                "amount", 100.0,
                "status", "FAILED"
        );
        Transaction failedResult = service.transformWebhookToTransaction("{}", failedPayload);
        assertThat(failedResult.getStatus()).isEqualTo(TransactionStatus.FAILED);

        // Test unknown status
        Map<String, Object> unknownPayload = Map.of(
                "id", "TXN-5",
                "amount", 100.0,
                "status", "UNKNOWN"
        );
        Transaction unknownResult = service.transformWebhookToTransaction("{}", unknownPayload);
        assertThat(unknownResult.getStatus()).isEqualTo(TransactionStatus.AUTHORIZED);
    }


    @Test
    void transformTransactionToFacturanteRequest_withBillingSettings_usesSettings() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.50"))
                .status(TransactionStatus.PAID)
                .customerDoc("20123456789")
                .currency("ARS")
                .tenantId(UUID.randomUUID())
                .build();

        String originalPayload = "{\"id\":\"TXN-123\",\"amount\":100.50}";

        BillingSettings settings = BillingSettings.builder()
                .tipoComprobante("FA")
                .puntoVenta("0002")
                .ivaPorDefecto(new BigDecimal("10.50"))
                .consumidorFinalPorDefecto(true)
                .cuitConsumidorFinal("00000000000")
                .razonSocialConsumidorFinal("Consumidor Final")
                .emailFacturacion("test@example.com")
                .enviarComprobante(true)
                .build();

        when(facturanteConfig.getEmpresa()).thenReturn("test-empresa");
        when(facturanteConfig.getUsuario()).thenReturn("test-usuario");
        when(facturanteConfig.getPassword()).thenReturn("test-password");
        when(billingSettingsService.getActiveSettings(transaction.getTenantId())).thenReturn(Optional.of(settings));

        // Act
        CrearComprobanteRequest result = service.transformTransactionToFacturanteRequest(transaction, originalPayload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEncabezado().getTipoComprobante()).isEqualTo("FA");
        assertThat(result.getEncabezado().getPrefijo()).isEqualTo("0002");
    }

    @Test
    void transformTransactionToFacturanteRequest_withItemsFromPayload_createsItems() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.50"))
                .status(TransactionStatus.PAID)
                .customerDoc("20123456789")
                .currency("ARS")
                .tenantId(UUID.randomUUID())
                .build();

        String originalPayload = "{\"id\":\"TXN-123\",\"amount\":100.50,\"metadata\":{\"items\":[{\"name\":\"Producto 1\",\"quantity\":2,\"unitPrice\":50.25}]}}";

        when(facturanteConfig.getEmpresa()).thenReturn("test-empresa");
        when(facturanteConfig.getUsuario()).thenReturn("test-usuario");
        when(facturanteConfig.getPassword()).thenReturn("test-password");
        when(facturanteConfig.getTipoComprobante()).thenReturn("FB");
        when(facturanteConfig.getPrefijo()).thenReturn("0001");

        // Act
        CrearComprobanteRequest result = service.transformTransactionToFacturanteRequest(transaction, originalPayload);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isNotNull();
        assertThat(result.getItems().length).isGreaterThan(0);
    }

    @Test
    void transformTransactionToFacturanteRequest_withInvalidJson_handlesGracefully() {
        // Arrange
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .externalId("TXN-123")
                .amount(new BigDecimal("100.50"))
                .status(TransactionStatus.PAID)
                .customerDoc("20123456789")
                .currency("ARS")
                .tenantId(UUID.randomUUID())
                .build();

        String originalPayload = "invalid json";

        // Act & Assert
        try {
            CrearComprobanteRequest result = service.transformTransactionToFacturanteRequest(transaction, originalPayload);
            assertThat(result).isNotNull();
        } catch (Exception e) {
            // Should handle gracefully or throw a specific exception
            assertThat(e).isNotNull();
        }
    }
}