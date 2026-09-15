package com.kshetrify.backend.repository;

import com.kshetrify.backend.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationRepository
        extends JpaRepository<Verification, Long> {

    List<Verification> findByDocumentId(Long documentId);
}