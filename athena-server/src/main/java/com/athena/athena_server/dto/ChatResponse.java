package com.athena.athena_server.dto;

import java.time.Instant;

public record ChatResponse(
        String sessionId,
        String model,
        String message,
        Instant timestamp) {
}