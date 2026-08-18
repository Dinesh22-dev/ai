package com.athena.athena_server.dto;

public record AttachmentResponse(
        String id,
        String fileName,
        String contentType,
        long size) {
}