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
public class GetnetRefundRequest {
    
    @JsonProperty("amount")
    private Integer amount; // En centavos
}
