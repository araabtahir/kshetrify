package com.kshetrify.backend.service;

import com.kshetrify.backend.entity.Document;
import com.kshetrify.backend.entity.Verification;
import com.kshetrify.backend.repository.DocumentRepository;
import com.kshetrify.backend.repository.VerificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VerificationService {

    private final VerificationRepository verificationRepository;
    private final DocumentRepository documentRepository;

    public VerificationService(
            VerificationRepository verificationRepository,
            DocumentRepository documentRepository) {

        this.verificationRepository = verificationRepository;
        this.documentRepository = documentRepository;
    }

    public Verification verifyDocument(
            Long documentId,
            Long reviewerId,
            String action,
            String comments) {

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Document not found"
                                ));

        if (!action.equals("APPROVE")
                && !action.equals("REJECT")) {

            throw new RuntimeException(
                    "Action must be APPROVE or REJECT"
            );
        }

        Verification verification =
                new Verification();

        verification.setDocumentId(documentId);
        verification.setReviewerId(reviewerId);
        verification.setAction(action);
        verification.setComments(comments);
        verification.setVerifiedAt(
                LocalDateTime.now()
        );

        Verification saved =
                verificationRepository.save(verification);

        if (action.equals("APPROVE")) {
            document.setProcessingStatus(
                    "VERIFIED"
            );
        } else {
            document.setProcessingStatus(
                    "REJECTED"
            );
        }

        documentRepository.save(document);

        return saved;
    }

    public List<Verification> getVerifications(
            Long documentId) {

        return verificationRepository
                .findByDocumentId(documentId);
    }
}