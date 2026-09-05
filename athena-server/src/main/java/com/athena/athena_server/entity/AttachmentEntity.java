package com.athena.athena_server.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "attachments")
@Data
public class AttachmentEntity {

    @Id
    private String id; // the UUID you already generate

    private String fileName;
    private String contentType;
    private long size;
    private String storedPath;
    private Instant uploadedAt;

    // getters/setters
}