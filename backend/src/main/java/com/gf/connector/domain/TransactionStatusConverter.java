package com.gf.connector.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TransactionStatusConverter implements AttributeConverter<TransactionStatus, String> {

    @Override
    public String convertToDatabaseColumn(TransactionStatus status) {
        if (status == null) {
            return null;
        }
        return status.getCode();
    }

    @Override
    public TransactionStatus convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        
        // Normalizar a minúsculas para comparación case-insensitive
        String normalizedCode = code.toLowerCase().trim();
        
        for (TransactionStatus status : TransactionStatus.values()) {
            if (status.getCode().toLowerCase().equals(normalizedCode)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown TransactionStatus code: " + code);
    }
}