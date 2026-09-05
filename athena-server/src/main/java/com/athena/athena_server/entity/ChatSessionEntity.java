package com.athena.athena_server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_sessions")
@Data
public class ChatSessionEntity {

    @Id
    private String id;

    private String model;
    private Instant createdAt;
    private Instant updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ChatMessageEntity> messages = new ArrayList<>();

    // getters/setters
}