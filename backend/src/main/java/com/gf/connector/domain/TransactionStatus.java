package com.gf.connector.domain;

public enum TransactionStatus {
    PENDING("pending", "Pendiente"),
    AUTHORIZED("authorized", "Autorizada"),
    PAID("paid", "Pagada"),
    PENDING_BILLING_CONFIRMATION("pending_billing_confirmation", "Pendiente de Confirmación de Facturación"),
    NO_BILLING_REQUIRED("no_billing_required", "Sin Facturación Requerida"),
    REFUNDED("refunded", "Reembolsada"),
    FAILED("failed", "Fallida");

    private final String code;
    private final String description;

    TransactionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de transacción no válido: " + code);
    }
    
    // Método para que Spring pueda convertir desde string
    public static TransactionStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        
        // Intentar primero por nombre del enum (REFUNDED, PAID, etc.)
        try {
            return TransactionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si falla, intentar por código (refunded, paid, etc.)
            return fromCode(value.toLowerCase());
        }
    }

    @Override
    public String toString() {
        return code;
    }
}