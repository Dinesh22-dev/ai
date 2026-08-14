package com.athena.ai;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ChatSession {

    private final String sessionId;
    private final String model;
    private final Instant createdAt;
    private Instant updatedAt;

    private final List<ChatMessage> messages = new ArrayList<>();

    public ChatSession(String sessionId, String model) {
        this.sessionId = sessionId;
        this.model = model;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addUserMessage(String message) {
        messages.add(new ChatMessage("user", message));
        updatedAt = Instant.now();
    }

    public void addAssistantMessage(String message) {
        messages.add(new ChatMessage("assistant", message));
        updatedAt = Instant.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getModel() {
        return model;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ChatMessage> getMessages() {
        return List.copyOf(messages);
    }
}