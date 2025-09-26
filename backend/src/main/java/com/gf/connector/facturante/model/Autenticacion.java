package com.gf.connector.facturante.model;

import lombok.Data;

@Data
public class Autenticacion {
    private String empresa;
    private String usuario;
    private String hash; // password
}