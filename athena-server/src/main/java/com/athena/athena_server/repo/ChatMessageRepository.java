package com.athena.athena_server.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.athena.athena_server.entity.ChatMessageEntity;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findBySessionIdOrderBySequenceAsc(String sessionId);
}