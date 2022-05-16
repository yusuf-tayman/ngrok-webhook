import base.BaseWebHook;
import base.Log;
import base.WebhookRequest;
import org.testng.annotations.Test;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.Stack;

import static io.restassured.RestAssured.given;

public class ReceivedWebhookTests implements BaseWebHook {

    String webhookUrl;
    String requestBody = "{\n" +
            "  \"title\": \"foo\",\n" +
            "  \"body\": \"bar\",\n" +
            "  \"userId\": \"1\" \n}";

    @Test
    public void testReceivedWebhookRequest() {
        startHttpServer();
        webhookUrl = getPublicUrl() + "/";
        RestAssured.baseURI = webhookUrl;
        Response response = given()
                .contentType(ContentType.JSON)
                .and()
                .body(requestBody)
                .when()
                .post("/")
                .then()
                .extract().response();
        Stack<WebhookRequest> receivedRequests = waitFor("/");
        Log.info("Received requests: " + receivedRequests.get(0).getBody());
        Log.info(receivedRequests.size() + " requests received");
    }
}