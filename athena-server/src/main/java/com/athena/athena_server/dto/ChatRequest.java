package com.athena.athena_server.dto;

import java.util.List;

public record ChatRequest(
                String sessionId,
                String model,
                String message,
                List<String> attachmentIds) {
}