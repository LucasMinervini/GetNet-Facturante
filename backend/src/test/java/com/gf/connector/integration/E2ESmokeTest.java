package com.gf.connector.integration;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class E2ESmokeTest {

    @LocalServerPort
    int port;

    @BeforeAll
    static void setup() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @DisplayName("GET /api/health devuelve status ok")
    void health_ok() {
        given()
            .port(port)
        .when()
            .get("/api/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("ok"));
    }

    @Test
    @DisplayName("POST /api/test-flow crea transacción de prueba")
    void testFlow_createsTransaction() {
        given()
            .port(port)
        .when()
            .post("/api/test-flow")
        .then()
            .statusCode(200)
            .body("status", equalTo("success"))
            .body("transaction_id", notNullValue());
    }
}


