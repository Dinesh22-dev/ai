package com.athena.athena_server.dto;

public record ChatRequest(
        String sessionId,
        String model,
        String message) {
}