/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * En esta clase estan todos los metodos para interactuar on la API de Finisterra.
 * @author Jopi
 */
public class ApiController {
    
    private final HttpClient httpClient;
    private HttpResponse<String> response;
    private HttpRequest request;
    private final String apiServer;

    public ApiController(String apiURL, int apiPORT) {   
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        
        this.apiServer = apiURL + ":" + apiPORT;
    }
    
    public void sendGET() throws IOException, InterruptedException {

        request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(apiServer + "/api/v2"))
                .build();

        
        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // print status code
        System.out.println(response.statusCode());

        // print response body
        System.out.println(response.body());

    }
    
}
