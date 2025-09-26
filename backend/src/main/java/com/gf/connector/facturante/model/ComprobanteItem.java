package com.gf.connector.facturante.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ComprobanteItem {
    private String codigo;
    private String detalle;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal iva;
    private Boolean gravado;
    private BigDecimal total;
}