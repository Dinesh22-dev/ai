package com.athena.athena_server.controller;

import com.athena.ai.ChatSession;
import com.athena.ai.OllamaAiService;
import com.athena.athena_server.dto.ChatRequest;
import com.athena.athena_server.dto.ChatResponse;
import com.athena.athena_server.service.SessionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final OllamaAiService aiService;
    private final SessionService sessionService;

    public ChatController(
            @Value("${ollama.url}") String ollamaUrl,
            SessionService sessionService) {

        this.aiService = new OllamaAiService(ollamaUrl);
        this.sessionService = sessionService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        try {

            if (!aiService.modelExists(request.model())) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Model not found: " + request.model());
            }

            ChatSession session = sessionService.getSession(request.sessionId());

            if (session == null) {

                session = sessionService.createSession(
                        request.sessionId(),
                        request.model());

            } else if (!session.getModel().equals(request.model())) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Session is using model: " + session.getModel());
            }

            session.addUserMessage(request.message());

            String response = aiService.chat(
                    request.model(),
                    session);

            return new ChatResponse(
                    session.getSessionId(),
                    session.getModel(),
                    response,
                    Instant.now());

        } catch (ResponseStatusException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to communicate with Ollama",
                    e);
        }
    }
}