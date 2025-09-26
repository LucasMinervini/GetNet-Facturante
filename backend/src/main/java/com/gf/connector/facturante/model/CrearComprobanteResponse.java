package com.gf.connector.facturante.model;

import lombok.Data;

@Data
public class CrearComprobanteResponse {
    private String estado;
    private String[] mensajes;
    private String cae;
    private String numeroComprobante;
    private String fechaVencimientoCae;
    private String pdfUrl;
    private Boolean exitoso;
}