package de.hsuifa.xapi.xapi_core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.json.JSONArray;

public class RestClient {
    
    private HttpClient httpClient;
    private URI targetUriAbox;
    private URI targetUriTbox;

    /**
     * constructor
     * @param port
     */
    public RestClient(Integer port) {

        httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        try {
            this.targetUriAbox = new URI("http://localhost:" + port.toString() + "/triple");
            this.targetUriTbox = new URI("http://localhost:" + port.toString() + "/tbox");
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * method to request triples based on a triple pattern
     * @param requestJson
     * @return
     */ 
    public JSONArray getAboxTriples (JSONArray requestJson) {

        String requestBody = requestJson.toString();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(targetUriAbox)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            // .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        JSONArray returnJson = new JSONArray(response.body());
        return returnJson;

    }

    /**
     * method to retrieve complete tbox
     * @return
     */
    public JSONArray getTboxTriples ( ) {

        HttpRequest request = HttpRequest.newBuilder()
            .uri(targetUriTbox)
            .GET()
            // .header("Content-Type", "application/json")
            .build();

        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        JSONArray returnJson = new JSONArray(response.body());
        return returnJson;

    }

}
