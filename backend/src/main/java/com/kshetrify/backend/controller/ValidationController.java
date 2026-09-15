package com.kshetrify.backend.controller;

import com.kshetrify.backend.dto.ValidationSummary;
import com.kshetrify.backend.entity.ValidationResult;
import com.kshetrify.backend.service.ValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validation")
public class ValidationController {

    private final ValidationService validationService;

    public ValidationController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/{documentId}")
    public ResponseEntity<List<ValidationResult>> validateDocument(
            @PathVariable Long documentId) {

        List<ValidationResult> results =
                validationService.validateDocument(documentId);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<List<ValidationResult>> getValidationResults(
            @PathVariable Long documentId) {

        List<ValidationResult> results =
                validationService.getValidationResults(documentId);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/{documentId}/summary")
    public ResponseEntity<ValidationSummary> getValidationSummary(
            @PathVariable Long documentId) {

        ValidationSummary summary =
                validationService.getValidationSummary(documentId);

        return ResponseEntity.ok(summary);
    }
}