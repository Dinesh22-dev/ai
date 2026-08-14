package com.athena.athena_server.controller;

import com.athena.ai.ChatSession;
import com.athena.athena_server.service.SessionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public Collection<ChatSession> getSessions() {

        return sessionService.getAllSessions();
    }

    @GetMapping("/{sessionId}")
    public ChatSession getSession(
            @PathVariable String sessionId) {

        ChatSession session = sessionService.getSession(sessionId);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Session not found: " + sessionId);
        }

        return session;
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId) {

        if (!sessionService.exists(sessionId)) {
            return ResponseEntity.notFound().build();
        }

        sessionService.deleteSession(sessionId);

        return ResponseEntity.noContent().build();
    }
}