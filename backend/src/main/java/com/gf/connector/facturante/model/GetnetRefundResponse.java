package com.gf.connector.facturante.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetnetRefundResponse {
    
    @JsonProperty("payment_id")
    private String paymentId;
    
    @JsonProperty("authorization_code")
    private String authorizationCode;
    
    @JsonProperty("status")
    private String status; // "Refunded"
    
    @JsonProperty("transaction_datetime")
    private OffsetDateTime transactionDatetime;
    
    @JsonProperty("generated_by")
    private UUID generatedBy;
}
