package com.github.gpor0;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.wildfly.common.Assert.assertTrue;

/**
 * author: gpor0@github.com
 */
@QuarkusTest
public class ApiResourceTest {

    @Test
    public void testSingleClientAccess() throws InterruptedException {
        String clientId = "1";
        //first request
        Long firstReqCurMillis = null;
        for (int i = 0; i < 5; i++) {
            given()
                    .when().get("/api?clientId=" + clientId)
                    .then().statusCode(200);
            if (i == 0) {
                firstReqCurMillis = System.currentTimeMillis();
            }
        }

        long now = System.currentTimeMillis();
        assertTrue(now - 5000 < firstReqCurMillis);
        //second request immediate after first should fail
        given()
                .when().get("/api?clientId=" + clientId)
                .then()
                .statusCode(503);

        long elapsed = System.currentTimeMillis() - firstReqCurMillis;
        Thread.sleep(5000 - elapsed);
        given()
                .when().get("/api?clientId=" + clientId)
                .then().statusCode(200);
    }

}
