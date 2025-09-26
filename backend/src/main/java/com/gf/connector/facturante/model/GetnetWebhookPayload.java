package com.gf.connector.facturante.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetnetWebhookPayload {
    
    @JsonProperty("payment_intent_id")
    private String paymentIntentId;
    
    @JsonProperty("checkout_id")
    private String checkoutId;
    
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("mode")
    private String mode;
    
    @JsonProperty("seller")
    private GetnetSeller seller;
    
    @JsonProperty("customer")
    private GetnetCustomer customer;
    
    @JsonProperty("shipping")
    private GetnetShipping shipping;
    
    @JsonProperty("payment")
    private GetnetPayment payment;
    
    @JsonProperty("pickup_store")
    private Boolean pickupStore;
    
    @JsonProperty("shipping_method")
    private String shippingMethod;
    
    @JsonProperty("soft_descriptor")
    private String softDescriptor;
    
    @JsonProperty("reference_code")
    private String referenceCode;
    
    @JsonProperty("product")
    private List<GetnetProduct> products;
    
    @JsonProperty("device_fingerprint")
    private String deviceFingerprint;
    
    @JsonProperty("created_at")
    private OffsetDateTime createdAt;
    
    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetSeller {
        @JsonProperty("id")
        private String id;
        
        @JsonProperty("trade_name")
        private String tradeName;
        
        @JsonProperty("merchant_document")
        private String merchantDocument;
        
        @JsonProperty("settings")
        private GetnetSellerSettings settings;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetSellerSettings {
        @JsonProperty("notification_url_configured")
        private Boolean notificationUrlConfigured;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetCustomer {
        @JsonProperty("customer_id")
        private String customerId;
        
        @JsonProperty("first_name")
        private String firstName;
        
        @JsonProperty("last_name")
        private String lastName;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("email")
        private String email;
        
        @JsonProperty("document_type")
        private String documentType;
        
        @JsonProperty("document_number")
        private String documentNumber;
        
        @JsonProperty("phone_number")
        private String phoneNumber;
        
        @JsonProperty("gender")
        private String gender;
        
        @JsonProperty("checked_email")
        private Boolean checkedEmail;
        
        @JsonProperty("billing_address")
        private GetnetAddress billingAddress;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetShipping {
        @JsonProperty("first_name")
        private String firstName;
        
        @JsonProperty("last_name")
        private String lastName;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("phone_number")
        private String phoneNumber;
        
        @JsonProperty("shipping_amount")
        private Integer shippingAmount;
        
        @JsonProperty("address")
        private GetnetAddress address;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetPayment {
        @JsonProperty("method")
        private String method; // credit, debit
        
        @JsonProperty("amount")
        private Integer amount;
        
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("installment")
        private GetnetInstallment installment;
        
        @JsonProperty("payment_method")
        private GetnetPaymentMethod paymentMethod;
        
        @JsonProperty("result")
        private GetnetPaymentResult result;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetInstallment {
        @JsonProperty("quote_id")
        private String quoteId;
        
        @JsonProperty("schema")
        private String schema;
        
        @JsonProperty("type")
        private String type; // no_interest, with_interest
        
        @JsonProperty("number")
        private Integer number;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetPaymentMethod {
        @JsonProperty("token_id")
        private String tokenId;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetPaymentResult {
        @JsonProperty("payment_id")
        private String paymentId;
        
        @JsonProperty("status")
        private String status; // Authorized, Denied
        
        @JsonProperty("authorization_code")
        private String authorizationCode;
        
        @JsonProperty("transaction_datetime")
        private OffsetDateTime transactionDatetime;
        
        @JsonProperty("return_message")
        private String returnMessage;
        
        @JsonProperty("return_code")
        private String returnCode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetProduct {
        @JsonProperty("product_type")
        private String productType;
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("value")
        private Integer value;
        
        @JsonProperty("quantity")
        private Integer quantity;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetAddress {
        @JsonProperty("street")
        private String street;
        
        @JsonProperty("number")
        private String number;
        
        @JsonProperty("complement")
        private String complement;
        
        @JsonProperty("district")
        private String district;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("state")
        private String state;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("postal_code")
        private String postalCode;
        
        @JsonProperty("reference")
        private String reference;
    }
}
