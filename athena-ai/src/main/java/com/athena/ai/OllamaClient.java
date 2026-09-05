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

                ObjectNode requestBody = objectMapper.createObjectNode();

                requestBody.put("model", model);
                requestBody.put("prompt", prompt);
                requestBody.put("stream", false);

                // Generation options
                ObjectNode options = requestBody.putObject("options");
                options.put("num_predict", 8192);

                // Convert to JSON only after building the complete object
                String json = requestBody.toString();

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(ollamaUrl + "/api/generate"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
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

                JsonNode jsonResponse = objectMapper.readTree(response.body());

                return jsonResponse
                                .path("response")
                                .asText();
        }

        public String chat(String model, List<ChatMessage> messages)
                        throws IOException, InterruptedException {

                ObjectNode requestBody = objectMapper.createObjectNode();

                requestBody.put("model", model);
                requestBody.put("stream", false);

                // =====================================================
                // Generation options
                // =====================================================

                ObjectNode options = requestBody.putObject("options");

                // Maximum number of tokens generated in the response.
                // Increase this for large Java/code responses.
                options.put("num_predict", 8192);

                // =====================================================
                // Messages
                // =====================================================

                ArrayNode messagesArray = requestBody.putArray("messages");

                for (ChatMessage message : messages) {

                        ObjectNode messageNode = messagesArray.addObject();

                        messageNode.put("role", message.role());
                        messageNode.put("content", message.content());
                }

                // =====================================================
                // Debug request
                // =====================================================

                System.out.println("Sending chat request to Ollama");
                System.out.println("Model: " + model);
                System.out.println("Messages: " + messages.size());

                // =====================================================
                // HTTP request
                // =====================================================

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(ollamaUrl + "/api/chat"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(
                                                requestBody.toString()))
                                .build();

                HttpResponse<String> response = httpClient.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());

                // =====================================================
                // HTTP error
                // =====================================================

                if (response.statusCode() != 200) {

                        throw new IOException(
                                        "Ollama returned HTTP "
                                                        + response.statusCode()
                                                        + ": "
                                                        + response.body());
                }

                // =====================================================
                // Parse response
                // =====================================================

                JsonNode json = objectMapper.readTree(response.body());

                // =====================================================
                // VERY IMPORTANT:
                // Check why Ollama stopped generating
                // =====================================================

                boolean done = json.path("done").asBoolean(false);

                String doneReason = json.path("done_reason").asText("");

                int promptEvalCount = json.path("prompt_eval_count").asInt(0);

                int evalCount = json.path("eval_count").asInt(0);

                System.out.println("======================================");
                System.out.println("Ollama generation finished");
                System.out.println("Done: " + done);
                System.out.println("Done reason: " + doneReason);
                System.out.println("Prompt tokens: " + promptEvalCount);
                System.out.println("Output tokens: " + evalCount);
                System.out.println("======================================");

                // =====================================================
                // Extract response
                // =====================================================

                String content = json
                                .path("message")
                                .path("content")
                                .asText();

                if (content == null || content.isBlank()) {

                        throw new IOException(
                                        "Ollama returned an empty response: "
                                                        + response.body());
                }

                return content;
        }
}