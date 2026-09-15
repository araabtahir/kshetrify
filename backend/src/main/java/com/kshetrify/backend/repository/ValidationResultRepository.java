package com.kshetrify.backend.repository;

import com.kshetrify.backend.entity.ValidationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ValidationResultRepository
        extends JpaRepository<ValidationResult, Long> {

    List<ValidationResult> findByDocumentId(Long documentId);
}