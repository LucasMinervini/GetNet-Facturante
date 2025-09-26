package com.gf.connector.facturante.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetnetPaymentIntent {
    
    @JsonProperty("mode")
    private String mode = "instant";
    
    @JsonProperty("order_id")
    private String orderId;
    
    @JsonProperty("payment")
    private GetnetPayment payment;
    
    @JsonProperty("product")
    private List<GetnetProduct> products;
    
    @JsonProperty("customer")
    private GetnetCustomer customer;
    
    @JsonProperty("shipping")
    private GetnetShipping shipping;
    
    @JsonProperty("pickup_store")
    private Boolean pickupStore = false;
    
    @JsonProperty("shipping_method")
    private String shippingMethod;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetPayment {
        @JsonProperty("currency")
        private String currency;
        
        @JsonProperty("amount")
        private Integer amount; // En centavos
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetnetProduct {
        @JsonProperty("product_type")
        private String productType = "physical_goods";
        
        @JsonProperty("title")
        private String title;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("value")
        private Integer value; // En centavos
        
        @JsonProperty("quantity")
        private Integer quantity;
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
        private Boolean checkedEmail = false;
        
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
