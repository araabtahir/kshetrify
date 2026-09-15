package com.kshetrify.backend.repository;

import com.kshetrify.backend.entity.ExtractedField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractedFieldRepository
        extends JpaRepository<ExtractedField, Long> {

    List<ExtractedField> findByDocumentId(Long documentId);
}