package com.athena.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class OllamaClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String ollamaUrl;

    public OllamaClient(String ollamaUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.ollamaUrl = ollamaUrl;
    }

    public List<ModelInfo> listModels()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl + "/api/tags"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ollama returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());

        List<ModelInfo> models = new ArrayList<>();

        for (JsonNode model : json.path("models")) {

            String name = model.path("name").asText();
            long size = model.path("size").asLong();
            String modifiedAt = model.path("modified_at").asText();

            if (!name.isBlank()) {
                models.add(new ModelInfo(
                        name,
                        size,
                        modifiedAt));
            }
        }

        return models;
    }

    public String generate(String model, String prompt)
            throws IOException, InterruptedException {

        String requestBody = objectMapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", false)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ollama returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());

        return json.path("response").asText();
    }

    public String chat(String model, List<ChatMessage> messages)
            throws IOException, InterruptedException {

        ObjectNode requestBody = objectMapper.createObjectNode();

        requestBody.put("model", model);
        requestBody.put("stream", false);

        ArrayNode messagesArray = requestBody.putArray("messages");

        for (ChatMessage message : messages) {

            ObjectNode messageNode = messagesArray.addObject();

            messageNode.put("role", message.role());
            messageNode.put("content", message.content());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl + "/api/chat"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ollama returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());

        return json.path("message")
                .path("content")
                .asText();
    }
}