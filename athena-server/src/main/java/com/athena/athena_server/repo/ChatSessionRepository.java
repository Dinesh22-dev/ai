package com.athena.athena_server.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.athena.athena_server.entity.ChatSessionEntity;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, String> {
}