package com.athena.athena_server.controller;

import com.athena.ai.ChatSession;
import com.athena.ai.OllamaAiService;
import com.athena.athena_server.dto.ChatRequest;
import com.athena.athena_server.dto.ChatResponse;
import com.athena.athena_server.service.AttachmentService;
import com.athena.athena_server.service.SessionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    // =====================================================
    // Services
    // =====================================================

    private final OllamaAiService aiService;

    private final SessionService sessionService;

    private final AttachmentService attachmentService;

    // =====================================================
    // Configuration
    // =====================================================

    private final String visionModel;

    private final List<String> visionModels;

    // =====================================================
    // Constructor
    // =====================================================

    public ChatController(
            @Value("${ollama.url}") String ollamaUrl,

            @Value("${ollama.vision-model:gemma3:4b}") String visionModel,

            @Value("${ollama.vision-models:gemma3:4b}") String visionModels,

            SessionService sessionService,

            AttachmentService attachmentService) {

        this.aiService = new OllamaAiService(ollamaUrl);

        this.sessionService = sessionService;

        this.attachmentService = attachmentService;

        this.visionModel = visionModel;

        this.visionModels = Arrays.stream(
                visionModels.split(","))
                .map(String::trim)
                .filter(model -> !model.isBlank())
                .toList();
    }

    // =====================================================
    // Chat
    // =====================================================

    @PostMapping("/chat")
    public ChatResponse chat(
            @RequestBody ChatRequest request) {

        try {

            // =================================================
            // Validate requested model
            // =================================================

            if (!aiService.modelExists(
                    request.model())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Model not found: " +
                                request.model());
            }

            // =================================================
            // Get or create session
            // =================================================

            ChatSession session = sessionService.getSession(
                    request.sessionId());

            if (session == null) {

                session = sessionService.createSession(
                        request.sessionId(),
                        request.model());

            } else if (!session.getModel()
                    .equals(request.model())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Session is using model: " +
                                session.getModel());
            }

            // =================================================
            // Original user message
            // =================================================

            String userMessage = request.message();

            // =================================================
            // Check attachments
            // =================================================

            if (request.attachmentIds() != null &&
                    !request.attachmentIds().isEmpty()) {

                // ---------------------------------------------
                // Check first attachment
                // ---------------------------------------------

                String firstAttachment = request.attachmentIds()
                        .get(0);

                // =================================================
                // IMAGE ATTACHMENT
                // =================================================

                if (attachmentService.isImage(
                        firstAttachment)) {

                    return handleImageMessage(
                            request,
                            session,
                            userMessage,
                            firstAttachment);
                }

                // =================================================
                // TEXT / DOCUMENT ATTACHMENTS
                // =================================================

                StringBuilder attachmentContext = new StringBuilder();

                attachmentContext.append(
                        "\n\nAttached files:\n");

                for (String attachmentId : request.attachmentIds()) {

                    String content = attachmentService.readText(
                            attachmentId);

                    attachmentContext
                            .append("\n--- ")
                            .append(attachmentId)
                            .append(" ---\n")
                            .append(content)
                            .append("\n--- End file ---\n");
                }

                userMessage += attachmentContext;
            }

            // =================================================
            // Normal text conversation
            // =================================================

            session.addUserMessage(
                    userMessage);

            String response = aiService.chat(
                    request.model(),
                    session);

            return new ChatResponse(
                    session.getSessionId(),
                    request.model(),
                    response,
                    Instant.now());

        } catch (ResponseStatusException e) {

            throw e;

        } catch (Exception e) {

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to communicate with Ollama",
                    e);
        }
    }

    // =====================================================
    // Image message
    // =====================================================

    private ChatResponse handleImageMessage(
            ChatRequest request,
            ChatSession session,
            String userMessage,
            String attachmentId)
            throws Exception {

        // =================================================
        // Determine image model dynamically
        // =================================================

        String imageModel = selectVisionModel(
                request.model());

        System.out.println(
                "Image attachment detected.");

        System.out.println(
                "Requested model: " +
                        request.model());

        System.out.println(
                "Vision model selected: " +
                        imageModel);

        // =================================================
        // Validate vision model
        // =================================================

        if (!aiService.modelExists(
                imageModel)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vision model not installed: " +
                            imageModel);
        }

        // =================================================
        // Read image
        // =================================================

        byte[] imageBytes = attachmentService.readBytes(
                attachmentId);

        // =================================================
        // Send image to vision model
        // =================================================

        String response = aiService.chatWithImage(
                imageModel,
                userMessage,
                imageBytes);

        // =================================================
        // Store conversation
        // =================================================

        session.addUserMessage(
                userMessage +
                        "\n[Image attached]");

        session.addAssistantMessage(
                response);

        // =================================================
        // Return response
        // =================================================

        return new ChatResponse(
                session.getSessionId(),
                imageModel,
                response,
                Instant.now());
    }

    // =====================================================
    // Select vision model
    // =====================================================

    private String selectVisionModel(
            String requestedModel) {

        // -------------------------------------------------
        // User selected a vision-capable model
        // -------------------------------------------------

        if (isVisionModel(
                requestedModel)) {

            return requestedModel;
        }

        // -------------------------------------------------
        // User selected a text-only model
        // -------------------------------------------------

        return visionModel;
    }

    // =====================================================
    // Check vision capability
    // =====================================================

    private boolean isVisionModel(
            String model) {

        if (model == null ||
                model.isBlank()) {

            return false;
        }

        return visionModels.stream()
                .anyMatch(
                        configuredModel -> configuredModel.equalsIgnoreCase(
                                model));
    }
}