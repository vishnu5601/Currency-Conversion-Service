package org.project.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class OrderResourceIT {

    // ---------- POST /api/v1/orders ----------

    @Test
    void createOrder_validRequest_returns201WithConvertedAmount() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerId": "IT-CUST-1",
                          "amountUSD": 150.00,
                          "targetCurrency": "EUR"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(201)
                .body("orderId", notNullValue())
                .body("customerId", equalTo("IT-CUST-1"))
                .body("amountUSD", equalTo(150.00f))
                .body("targetCurrency", equalTo("EUR"))
                .body("convertedAmount", notNullValue())
                .body("status", equalTo("PROCESSED"))
                .body("createdAt", notNullValue());
    }

    @Test
    void createOrder_lowercaseCurrency_isNormalizedToUppercase() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerId": "IT-CUST-2",
                          "amountUSD": 100.00,
                          "targetCurrency": "gbp"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(201)
                .body("targetCurrency", equalTo("GBP"));
    }

    @Test
    void createOrder_missingCustomerId_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "amountUSD": 100.00,
                          "targetCurrency": "EUR"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(400)
                .body("status", equalTo(400));
    }

    @Test
    void createOrder_negativeAmount_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerId": "IT-CUST-3",
                          "amountUSD": -50.00,
                          "targetCurrency": "EUR"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(400);
    }

    @Test
    void createOrder_invalidCurrencyFormat_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerId": "IT-CUST-4",
                          "amountUSD": 50.00,
                          "targetCurrency": "EURO"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(400);
    }

    @Test
    void createOrder_unknownCurrencyCode_returns400WithMessage() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerId": "IT-CUST-5",
                          "amountUSD": 50.00,
                          "targetCurrency": "ZZZ"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(400)
                .body("message", equalTo("Unsupported or unknown target currency: ZZZ"));
    }

    @Test
    void createOrder_malformedJson_returns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerId": "IT-CUST-6",
                          "amountUSD": "not-a-number",
                          "targetCurrency": "EUR"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(400);
    }

    // ---------- GET /api/v1/orders/{id} ----------

    @Test
    void getOrder_existingId_returns200WithMatchingOrder() {
        int orderId =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                  "customerId": "IT-CUST-7",
                                  "amountUSD": 75.00,
                                  "targetCurrency": "JPY"
                                }
                                """)
                .when()
                        .post("/api/v1/orders")
                .then()
                        .statusCode(201)
                        .extract().path("orderId");

        given()
        .when()
                .get("/api/v1/orders/" + orderId)
        .then()
                .statusCode(200)
                .body("orderId", equalTo(orderId))
                .body("customerId", equalTo("IT-CUST-7"));
    }

    @Test
    void getOrder_nonExistentId_returns404() {
        given()
        .when()
                .get("/api/v1/orders/999999999")
        .then()
                .statusCode(404);
    }

    // ---------- GET /api/v1/orders ----------

    @Test
    void listOrders_returnsArray() {
        given()
        .when()
                .get("/api/v1/orders")
        .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
    }

    @Test
    void listOrders_filteredByCustomerId_returnsOnlyMatchingOrders() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "customerId": "IT-CUST-FILTER",
                          "amountUSD": 20.00,
                          "targetCurrency": "CAD"
                        }
                        """)
        .when()
                .post("/api/v1/orders")
        .then()
                .statusCode(201);

        given()
                .queryParam("customerId", "IT-CUST-FILTER")
        .when()
                .get("/api/v1/orders")
        .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("customerId", org.hamcrest.Matchers.everyItem(equalTo("IT-CUST-FILTER")));
    }
}
