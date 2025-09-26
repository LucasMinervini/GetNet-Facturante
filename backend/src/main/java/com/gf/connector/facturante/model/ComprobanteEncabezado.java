package com.gf.connector.facturante.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Calendar;

@Data
public class ComprobanteEncabezado {
    private String tipoComprobante; // FA/FB/FC
    private String prefijo; // Punto de venta
    private Integer condicionVenta; // 1=Contado
    private Integer bienes; // según AFIP: bienes/servicios
    private Calendar fechaHora;
    private BigDecimal subTotal;
    private BigDecimal totalNeto;
    private BigDecimal total;
    private BigDecimal percepciones;
}