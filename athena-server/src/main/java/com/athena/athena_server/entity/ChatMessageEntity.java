package com.athena.athena_server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private ChatSessionEntity session;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String content;

    private int sequence;
    private Instant createdAt;

    // getters/setters
}