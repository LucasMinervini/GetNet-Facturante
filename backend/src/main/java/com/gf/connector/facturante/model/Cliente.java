package com.gf.connector.facturante.model;

import lombok.Data;

@Data
public class Cliente {
    private String razonSocial;
    private Integer tipoDocumento; // 80=CUIT, 96=DNI, 99=CF
    private String nroDocumento;
    private String mailFacturacion;
    private Boolean enviarComprobante;
}