package com.gf.connector.facturante.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetnetPaymentIntentResponse {
    
    @JsonProperty("payment_intent_id")
    private String paymentIntentId;
}
