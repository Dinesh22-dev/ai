package com.athena.ai;

public record ChatMessage(
        String role,
        String content) {
}