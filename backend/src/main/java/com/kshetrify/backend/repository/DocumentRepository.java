package com.kshetrify.backend.repository;

import com.kshetrify.backend.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    long countByProcessingStatus(String processingStatus);
}