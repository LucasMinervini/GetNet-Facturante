package com.gf.connector.config;

import com.gf.connector.domain.TransactionStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TransactionStatusConverter implements Converter<String, TransactionStatus> {
    
    @Override
    public TransactionStatus convert(String source) {
        return TransactionStatus.fromString(source);
    }
}