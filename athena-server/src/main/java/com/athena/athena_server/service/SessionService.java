package com.athena.athena_server.service;

import com.athena.ai.ChatSession;
import com.athena.athena_server.entity.ChatMessageEntity;
import com.athena.athena_server.entity.ChatSessionEntity;
import com.athena.athena_server.repo.ChatSessionRepository;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;

@Service
public class SessionService {

    private final ChatSessionRepository sessionRepository;

    public SessionService(ChatSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public ChatSession createSession(String sessionId, String model) {
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setId(sessionId);
        entity.setModel(model);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        sessionRepository.save(entity);
        return toDomain(entity);
    }

    public ChatSession getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
                .map(this::toDomain)
                .orElse(null);
    }

    public void addMessage(String sessionId, String role, String content) {
        ChatSessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        ChatMessageEntity message = new ChatMessageEntity();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setSequence(session.getMessages().size());
        message.setCreatedAt(Instant.now());

        session.getMessages().add(message);
        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
    }

    public Collection<ChatSession> getAllSessions() {
        return sessionRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    public void deleteSession(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    public boolean exists(String sessionId) {
        return sessionRepository.existsById(sessionId);
    }

    private ChatSession toDomain(ChatSessionEntity entity) {
        ChatSession session = new ChatSession(entity.getId(), entity.getModel());
        for (ChatMessageEntity m : entity.getMessages()) {
            if ("user".equals(m.getRole())) {
                session.addUserMessage(m.getContent());
            } else {
                session.addAssistantMessage(m.getContent());
            }
        }
        return session;
    }
}