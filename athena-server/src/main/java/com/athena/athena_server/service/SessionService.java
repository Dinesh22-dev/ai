package com.athena.athena_server.service;

import com.athena.ai.ChatSession;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {

    private final ConcurrentHashMap<String, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatSession createSession(String sessionId, String model) {

        ChatSession session = new ChatSession(sessionId, model);

        sessions.put(sessionId, session);

        return session;
    }

    public ChatSession getSession(String sessionId) {

        return sessions.get(sessionId);
    }

    public Collection<ChatSession> getAllSessions() {

        return sessions.values();
    }

    public void deleteSession(String sessionId) {

        sessions.remove(sessionId);
    }

    public boolean exists(String sessionId) {

        return sessions.containsKey(sessionId);
    }
}