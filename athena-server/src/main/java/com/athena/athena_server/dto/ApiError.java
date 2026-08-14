package com.athena.athena_server.dto;

public record ApiError(
        String error,
        String message) {
}