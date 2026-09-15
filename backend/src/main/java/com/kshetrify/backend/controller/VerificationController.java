package com.kshetrify.backend.controller;

import com.kshetrify.backend.entity.Verification;
import com.kshetrify.backend.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(
            VerificationService verificationService) {

        this.verificationService = verificationService;
    }

    @PostMapping("/{documentId}")
    public ResponseEntity<Verification> verifyDocument(
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> request) {

        Long reviewerId =
                Long.valueOf(
                        request.get("reviewerId").toString()
                );

        String action =
                request.get("action").toString();

        String comments =
                request.get("comments") != null
                        ? request.get("comments").toString()
                        : "";

        Verification verification =
                verificationService.verifyDocument(
                        documentId,
                        reviewerId,
                        action,
                        comments
                );

        return ResponseEntity.ok(verification);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<List<Verification>> getVerifications(
            @PathVariable Long documentId) {

        return ResponseEntity.ok(
                verificationService
                        .getVerifications(documentId)
        );
    }
}