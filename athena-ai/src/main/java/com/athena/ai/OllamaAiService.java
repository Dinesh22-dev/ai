package com.athena.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OllamaAiService implements AiService {

    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String ollamaUrl;

    public OllamaAiService(String ollamaUrl) {
        this.ollamaUrl = ollamaUrl;
        this.ollamaClient = new OllamaClient(ollamaUrl);
    }

    public List<ModelInfo> listModels() throws Exception {
        return ollamaClient.listModels();
    }

    public boolean modelExists(String model) throws Exception {
        return listModels().stream()
                .anyMatch(m -> m.name().equals(model));
    }

    @Override
    public String generate(String prompt) throws Exception {
        throw new UnsupportedOperationException(
                "A model must be selected before generating a response.");
    }

    public String generate(String model, String prompt) throws Exception {
        return ollamaClient.generate(model, prompt);
    }

    public String chat(String model, ChatSession session)
            throws Exception {

        String response = ollamaClient.chat(
                model,
                session.getMessages());

        session.addAssistantMessage(response);

        return response;
    }

    public String chatWithImage(
            String model,
            String message,
            byte[] imageBytes) {

        try {

            String imageBase64 = Base64.getEncoder()
                    .encodeToString(imageBytes);

            // =====================================================
            // Build Ollama request using Jackson
            // =====================================================

            var root = objectMapper.createObjectNode();

            root.put(
                    "model",
                    model);

            root.put(
                    "stream",
                    false);

            var messages = root.putArray("messages");

            var userMessage = messages.addObject();

            userMessage.put(
                    "role",
                    "user");

            userMessage.put(
                    "content",
                    message);

            var images = userMessage.putArray("images");

            images.add(imageBase64);

            String json = objectMapper.writeValueAsString(root);

            System.out.println(
                    "Sending image to Ollama using model: " +
                            model);

            System.out.println(
                    "Image size: " +
                            imageBytes.length +
                            " bytes");

            // =====================================================
            // HTTP request
            // =====================================================

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(
                            URI.create(
                                    ollamaUrl +
                                            "/api/chat"))
                    .header(
                            "Content-Type",
                            "application/json")
                    .POST(
                            HttpRequest.BodyPublishers
                                    .ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers
                            .ofString());

            // =====================================================
            // Log Ollama response
            // =====================================================

            System.out.println(
                    "Ollama HTTP status: " +
                            response.statusCode());

            System.out.println(
                    "Ollama response: " +
                            response.body());

            // =====================================================
            // Check response
            // =====================================================

            if (response.statusCode() != 200) {

                throw new RuntimeException(
                        "Ollama returned HTTP " +
                                response.statusCode() +
                                ": " +
                                response.body());
            }

            // =====================================================
            // Parse response
            // =====================================================

            JsonNode responseRoot = objectMapper.readTree(
                    response.body());

            JsonNode messageNode = responseRoot.get("message");

            if (messageNode == null) {

                throw new RuntimeException(
                        "Ollama response does not contain 'message': " +
                                response.body());
            }

            String content = messageNode.path("content")
                    .asText();

            if (content == null ||
                    content.isBlank()) {

                throw new RuntimeException(
                        "Ollama returned an empty image response: " +
                                response.body());
            }

            return content;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to process image with Ollama",
                    e);
        }
    }

}