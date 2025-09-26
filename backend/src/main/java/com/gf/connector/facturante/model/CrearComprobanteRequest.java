package com.gf.connector.facturante.model;

import lombok.Data;

@Data
public class CrearComprobanteRequest {
    private Autenticacion autenticacion;
    private Cliente cliente;
    private ComprobanteEncabezado encabezado;
    private ComprobanteItem[] items;
}