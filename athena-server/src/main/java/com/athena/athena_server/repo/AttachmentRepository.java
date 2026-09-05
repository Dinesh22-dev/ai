package com.athena.athena_server.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.athena.athena_server.entity.AttachmentEntity;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, String> {
}
