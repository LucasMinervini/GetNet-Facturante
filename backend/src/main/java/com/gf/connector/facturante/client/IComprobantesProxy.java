package com.gf.connector.facturante.client;

import com.gf.connector.facturante.model.CrearComprobanteRequest;
import com.gf.connector.facturante.model.CrearComprobanteResponse;

public interface IComprobantesProxy {
    CrearComprobanteResponse crearComprobante(CrearComprobanteRequest request) throws Exception;
}