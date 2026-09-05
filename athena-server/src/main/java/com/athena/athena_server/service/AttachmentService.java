package com.athena.athena_server.service;

import com.athena.athena_server.dto.AttachmentResponse;
import com.athena.athena_server.entity.AttachmentEntity;
import com.athena.athena_server.repo.AttachmentRepository;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class AttachmentService {

    private final Path storageDirectory;
    private final AttachmentRepository attachmentRepository;
    private final Tika tika;

    public AttachmentService(
            @Value("${athena.storage.attachments:./athena-data/attachments}") String storageDirectory,
            AttachmentRepository attachmentRepository) {

        this.storageDirectory = Paths
                .get(storageDirectory)
                .toAbsolutePath()
                .normalize();

        this.attachmentRepository = attachmentRepository;
        this.tika = new Tika();

        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create attachment storage directory", e);
        }
    }

    // =====================================================
    // Store attachment
    // =====================================================

    public AttachmentResponse store(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment is empty");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "attachment";
        }

        String safeFileName = Paths.get(originalFileName)
                .getFileName()
                .toString();

        String id = UUID.randomUUID().toString();
        String storedFileName = id + "_" + safeFileName;

        Path destination = storageDirectory
                .resolve(storedFileName)
                .normalize();

        if (!destination.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("Invalid attachment filename");
        }

        // --- Copy file to disk ---
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store attachment", e);
        }

        // --- Determine content type BEFORE saving the entity ---
        String contentType = file.getContentType();

        if (contentType == null ||
                contentType.isBlank() ||
                "application/octet-stream".equals(contentType)) {
            try {
                contentType = tika.detect(destination);
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }
        }

        // --- Persist metadata ---
        AttachmentEntity entity = new AttachmentEntity();
        entity.setId(id);
        entity.setFileName(safeFileName);
        entity.setContentType(contentType);
        entity.setSize(file.getSize());
        entity.setStoredPath(destination.toString());
        entity.setUploadedAt(Instant.now());
        attachmentRepository.save(entity);

        return new AttachmentResponse(id, safeFileName, contentType, file.getSize());
    }

    // =====================================================
    // Find attachment (now via DB instead of directory scan)
    // =====================================================

    public Path getPath(String attachmentId) {

        if (attachmentId == null || attachmentId.isBlank()) {
            throw new IllegalArgumentException("Attachment ID is required");
        }

        AttachmentEntity entity = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attachment not found: " + attachmentId));

        return Paths.get(entity.getStoredPath());
    }

    // =====================================================
    // Read attachment as text
    // =====================================================

    public String readText(String attachmentId) {

        Path path = getPath(attachmentId);

        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();

            try (InputStream inputStream = Files.newInputStream(path)) {
                parser.parse(inputStream, handler, metadata, context);
            }

            String text = handler.toString();

            if (text == null || text.isBlank()) {
                return "[No readable text was found in this attachment.]";
            }

            return text.trim();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to extract text from attachment: " + path.getFileName(), e);
        }
    }

    // =====================================================
    // Image check
    // =====================================================

    public boolean isImage(String attachmentId) {

        Path path = getPath(attachmentId);

        String fileName = path.getFileName()
                .toString()
                .toLowerCase();

        return fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".webp")
                || fileName.endsWith(".gif")
                || fileName.endsWith(".bmp");
    }

    // =====================================================
    // Read raw bytes
    // =====================================================

    public byte[] readBytes(String attachmentId) {

        Path path = getPath(attachmentId);

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read attachment", e);
        }
    }

    // =====================================================
    // Content type lookup
    // =====================================================

    public String getContentType(String attachmentId) {

        AttachmentEntity entity = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Attachment not found: " + attachmentId));

        String contentType = entity.getContentType();

        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }

        return contentType;
    }
}