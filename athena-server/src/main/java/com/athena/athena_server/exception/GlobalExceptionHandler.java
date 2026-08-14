package com.athena.athena_server.exception;

import com.athena.athena_server.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException exception) {

        HttpStatus status = HttpStatus.valueOf(
                exception.getStatusCode().value());

        String error;

        if (status == HttpStatus.BAD_REQUEST) {

            error = "MODEL_NOT_FOUND";

        } else if (status == HttpStatus.CONFLICT) {

            error = "MODEL_MISMATCH";

        } else {

            error = "REQUEST_ERROR";
        }

        ApiError response = new ApiError(
                error,
                exception.getReason());

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(
            Exception exception) {

        ApiError response = new ApiError(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}