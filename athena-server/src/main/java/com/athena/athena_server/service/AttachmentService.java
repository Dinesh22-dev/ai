package com.athena.athena_server.service;

import com.athena.athena_server.dto.AttachmentResponse;

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

import java.util.UUID;

@Service
public class AttachmentService {

    private final Path storageDirectory;

    private final Tika tika;

    public AttachmentService(
            @Value("${athena.storage.attachments:./athena-data/attachments}") String storageDirectory) {

        this.storageDirectory = Paths
                .get(storageDirectory)
                .toAbsolutePath()
                .normalize();

        this.tika = new Tika();

        try {

            Files.createDirectories(
                    this.storageDirectory);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not create attachment storage directory",
                    e);
        }
    }

    // =====================================================
    // Store attachment
    // =====================================================

    public AttachmentResponse store(
            MultipartFile file) {

        if (file == null ||
                file.isEmpty()) {

            throw new IllegalArgumentException(
                    "Attachment is empty");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            originalFileName = "attachment";
        }

        /*
         * Remove any path supplied by the client.
         *
         * We only want the actual filename.
         */
        String safeFileName = Paths.get(originalFileName)
                .getFileName()
                .toString();

        String id = UUID.randomUUID().toString();

        String storedFileName = id + "_" + safeFileName;

        Path destination = storageDirectory
                .resolve(storedFileName)
                .normalize();

        /*
         * Prevent path traversal.
         */
        if (!destination.startsWith(
                storageDirectory)) {

            throw new IllegalArgumentException(
                    "Invalid attachment filename");
        }

        try (InputStream inputStream = file.getInputStream()) {

            Files.copy(
                    inputStream,
                    destination,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to store attachment",
                    e);
        }

        String contentType = file.getContentType();

        /*
         * If the browser/client did not provide
         * a content type, detect it ourselves.
         */
        if (contentType == null ||
                contentType.isBlank() ||
                "application/octet-stream".equals(contentType)) {

            try {

                contentType = tika.detect(
                        destination);

            } catch (IOException e) {

                contentType = "application/octet-stream";
            }
        }

        return new AttachmentResponse(
                id,
                safeFileName,
                contentType,
                file.getSize());
    }

    // =====================================================
    // Find attachment
    // =====================================================

    public Path getPath(
            String attachmentId) {

        if (attachmentId == null ||
                attachmentId.isBlank()) {

            throw new IllegalArgumentException(
                    "Attachment ID is required");
        }

        /*
         * Only UUID-style IDs are accepted.
         *
         * This prevents someone from attempting
         * path traversal through the attachment ID.
         */
        try {

            UUID.fromString(
                    attachmentId);

        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException(
                    "Invalid attachment ID");
        }

        try (var files = Files.list(
                storageDirectory)) {

            return files
                    .filter(path -> path.getFileName()
                            .toString()
                            .startsWith(
                                    attachmentId + "_"))
                    .findFirst()
                    .orElseThrow(
                            () -> new IllegalArgumentException(
                                    "Attachment not found: " +
                                            attachmentId));

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to find attachment",
                    e);
        }
    }

    // =====================================================
    // Read attachment as text
    // =====================================================

    public String readText(
            String attachmentId) {

        Path path = getPath(attachmentId);

        try {

            /*
             * AutoDetectParser lets Tika determine
             * the appropriate parser automatically.
             */
            AutoDetectParser parser = new AutoDetectParser();

            /*
             * -1 means unlimited text extraction.
             *
             * We will later introduce a configurable
             * maximum size to protect the model context.
             */
            BodyContentHandler handler = new BodyContentHandler(-1);

            Metadata metadata = new Metadata();

            ParseContext context = new ParseContext();

            try (InputStream inputStream = Files.newInputStream(path)) {

                parser.parse(
                        inputStream,
                        handler,
                        metadata,
                        context);
            }

            String text = handler.toString();

            if (text == null ||
                    text.isBlank()) {

                return "[No readable text was found in this attachment.]";
            }

            return text.trim();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to extract text from attachment: " +
                            path.getFileName(),
                    e);
        }
    }

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

    public byte[] readBytes(String attachmentId) {

        Path path = getPath(attachmentId);

        try {

            return Files.readAllBytes(path);

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to read attachment",
                    e);
        }
    }

    public String getContentType(String attachmentId) {

        Path path = getPath(attachmentId);

        try {

            String contentType = Files.probeContentType(path);

            if (contentType == null ||
                    contentType.isBlank()) {

                return "application/octet-stream";
            }

            return contentType;

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to determine attachment type",
                    e);
        }
    }
}