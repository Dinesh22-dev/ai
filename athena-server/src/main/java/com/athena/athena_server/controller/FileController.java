package com.athena.athena_server.controller;

import com.athena.athena_server.dto.AttachmentResponse;
import com.athena.athena_server.service.AttachmentService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final AttachmentService attachmentService;

    public FileController(
            AttachmentService attachmentService) {

        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    public AttachmentResponse upload(
            @RequestParam("file") MultipartFile file) {

        try {

            return attachmentService.store(file);

        } catch (IllegalArgumentException e) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage());

        } catch (Exception e) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store attachment",
                    e);
        }
    }
}