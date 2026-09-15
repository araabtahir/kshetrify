package com.kshetrify.backend.controller;

import com.kshetrify.backend.entity.Document;
import com.kshetrify.backend.repository.DocumentRepository;
import com.kshetrify.backend.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository repository;
    private final DocumentService documentService;

    private final Path uploadDirectory =
            Paths.get(
                    System.getProperty("user.dir"),
                    "uploads"
            ).toAbsolutePath();

    public DocumentController(
            DocumentRepository repository,
            DocumentService documentService) {

        this.repository = repository;
        this.documentService = documentService;
    }

    // Get all uploaded documents
    @GetMapping
    public List<Document> getAllDocuments() {
        return repository.findAll();
    }

    // Upload document
    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException(
                    "Please select a document."
            );
        }

        // Create uploads folder if it does not exist
        Files.createDirectories(uploadDirectory);

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null ||
                originalFileName.isBlank()) {

            throw new RuntimeException(
                    "Invalid file name."
            );
        }

        // Remove any path information from file name
        String fileName =
                Paths.get(originalFileName)
                        .getFileName()
                        .toString();

        // Create unique file name to avoid overwriting
        String uniqueFileName =
                System.currentTimeMillis()
                        + "_" + fileName;

        Path filePath =
                uploadDirectory.resolve(uniqueFileName);

        // Save uploaded file
        Files.copy(
                file.getInputStream(),
                filePath
        );

        // Create database document record
        Document document = new Document();

        document.setFileName(fileName);

        document.setFilePath(
                filePath.toAbsolutePath().toString()
        );

        document.setUploadTime(
                LocalDateTime.now()
        );

        document.setProcessingStatus(
                "UPLOADED"
        );

        Document savedDocument =
                repository.save(document);

        return Map.of(
                "message",
                "Document uploaded successfully",

                "documentId",
                savedDocument.getId(),

                "fileName",
                savedDocument.getFileName(),

                "processingStatus",
                savedDocument.getProcessingStatus()
        );
    }

    // Process document using Python OCR service
    @PostMapping("/{documentId}/process")
    public Map<String, Object> processDocument(
            @PathVariable Long documentId) {

        String result =
                documentService.processDocument(
                        documentId
                );

        return Map.of(
                "documentId",
                documentId,

                "message",
                "OCR processing completed",

                "result",
                result
        );
    }

    // Dashboard document statistics
    @GetMapping("/stats")
    public Map<String, Long> getStats() {

        long pending =
                repository.countByProcessingStatus(
                        "HUMAN_VERIFICATION_REQUIRED"
                );

        long verified =
                repository.countByProcessingStatus(
                        "VERIFIED"
                );

        long rejected =
                repository.countByProcessingStatus(
                        "REJECTED"
                );

        return Map.of(
                "pendingVerification",
                pending,

                "verified",
                verified,

                "rejected",
                rejected
        );
    }
}