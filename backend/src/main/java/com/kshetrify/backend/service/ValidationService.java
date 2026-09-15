package com.kshetrify.backend.service;

import com.kshetrify.backend.dto.ValidationSummary;
import com.kshetrify.backend.entity.Document;
import com.kshetrify.backend.entity.ExtractedField;
import com.kshetrify.backend.entity.LandRecord;
import com.kshetrify.backend.entity.ValidationResult;
import com.kshetrify.backend.repository.DocumentRepository;
import com.kshetrify.backend.repository.ExtractedFieldRepository;
import com.kshetrify.backend.repository.LandRecordRepository;
import com.kshetrify.backend.repository.ValidationResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValidationService {

    private final ExtractedFieldRepository extractedFieldRepository;
    private final LandRecordRepository landRecordRepository;
    private final ValidationResultRepository validationResultRepository;
    private final DocumentRepository documentRepository;

    public ValidationService(
            ExtractedFieldRepository extractedFieldRepository,
            LandRecordRepository landRecordRepository,
            ValidationResultRepository validationResultRepository,
            DocumentRepository documentRepository) {

        this.extractedFieldRepository = extractedFieldRepository;
        this.landRecordRepository = landRecordRepository;
        this.validationResultRepository = validationResultRepository;
        this.documentRepository = documentRepository;
    }

    public List<ValidationResult> validateDocument(Long documentId) {

        List<ExtractedField> fields =
                extractedFieldRepository.findByDocumentId(documentId);

        if (fields.isEmpty()) {
            throw new RuntimeException(
                    "No extracted fields found for document " + documentId
            );
        }

        LandRecord masterRecord =
                findMasterRecord(fields);

        Document document =
                documentRepository.findById(documentId)
                        .orElseThrow(() ->
                                new RuntimeException("Document not found"));

        document.setLandRecordId(masterRecord.getId());
        documentRepository.save(document);

        validationResultRepository.deleteAll(
                validationResultRepository.findByDocumentId(documentId)
        );

        for (ExtractedField field : fields) {

            String referenceValue =
                    getReferenceValue(
                            field.getFieldName(),
                            masterRecord
                    );

            if (referenceValue == null) {
                continue;
            }

            ValidationResult result =
                    compareField(
                            documentId,
                            field,
                            referenceValue
                    );

            validationResultRepository.save(result);
        }

        return validationResultRepository
                .findByDocumentId(documentId);
    }

    private LandRecord findMasterRecord(
        List<ExtractedField> fields) {

    String surveyNumber = null;
    String ownerName = null;
    String mutationNumber = null;

    for (ExtractedField field : fields) {

        if ("survey_number".equals(field.getFieldName())) {
            surveyNumber = field.getExtractedValue();
        }

        if ("owner_name".equals(field.getFieldName())) {
            ownerName = field.getExtractedValue();
        }

        if ("mutation_number".equals(field.getFieldName())) {
            mutationNumber = field.getExtractedValue();
        }
    }

    if (surveyNumber != null) {

        var record =
                landRecordRepository
                        .findBySurveyNumber(
                                surveyNumber.trim()
                        );

        if (record.isPresent()) {
            return record.get();
        }
    }

    if (mutationNumber != null) {

        for (LandRecord record :
                landRecordRepository.findAll()) {

            if (record.getMutationNumber() != null
                    && record.getMutationNumber()
                    .equalsIgnoreCase(
                            mutationNumber.trim()
                    )) {

                return record;
            }
        }
    }

    if (ownerName != null) {

        var record =
                landRecordRepository
                        .findByOwnerNameIgnoreCase(
                                ownerName.trim()
                        );

        if (record.isPresent()) {
            return record.get();
        }
    }

    throw new RuntimeException(
            "Unable to automatically identify master land record"
    );
}

    public List<ValidationResult> getValidationResults(
            Long documentId) {

        return validationResultRepository
                .findByDocumentId(documentId);
    }

    public ValidationSummary getValidationSummary(
            Long documentId) {

        List<ValidationResult> results =
                validationResultRepository
                        .findByDocumentId(documentId);

        if (results.isEmpty()) {
            throw new RuntimeException(
                    "No validation results found for document "
                            + documentId
            );
        }

        int matched = 0;
        int mismatched = 0;

        for (ValidationResult result : results) {

            if ("MATCH".equals(result.getValidationStatus())) {
                matched++;
            }

            if ("MISMATCH".equals(result.getValidationStatus())) {
                mismatched++;
            }
        }

        String overallRisk;

        if (mismatched >= 2) {
            overallRisk = "HIGH";
        } else if (mismatched == 1) {
            overallRisk = "MEDIUM";
        } else {
            overallRisk = "LOW";
        }

        String verificationStatus =
                mismatched > 0
                        ? "HUMAN_VERIFICATION_REQUIRED"
                        : "VERIFICATION_NOT_REQUIRED";

        ValidationSummary summary =
                new ValidationSummary();

        summary.setDocumentId(documentId);
        summary.setOverallRisk(overallRisk);
        summary.setVerificationStatus(verificationStatus);
        summary.setTotalFields(results.size());
        summary.setMatchedFields(matched);
        summary.setMismatchedFields(mismatched);
        summary.setResults(results);

        return summary;
    }

    private String getReferenceValue(
            String fieldName,
            LandRecord record) {

        return switch (fieldName) {

            case "owner_name" ->
                    record.getOwnerName();

            case "survey_number" ->
                    record.getSurveyNumber();

            case "area" ->
                    record.getArea() != null
                            ? record.getArea().toPlainString()
                            : null;

            case "area_unit" ->
                    record.getAreaUnit();

            case "village" ->
                    record.getVillage();

            case "tehsil" ->
                    record.getTehsil();

            case "district" ->
                    record.getDistrict();

            case "mutation_number" ->
                    record.getMutationNumber();

            default -> null;
        };
    }

    private ValidationResult compareField(
            Long documentId,
            ExtractedField field,
            String referenceValue) {

        String extractedValue =
                field.getExtractedValue();

        boolean matched;

        if ("area".equals(field.getFieldName())) {

            try {

                double extracted =
                        Double.parseDouble(extractedValue);

                double reference =
                        Double.parseDouble(referenceValue);

                matched =
                        Math.abs(extracted - reference)
                                < 0.0001;

            } catch (NumberFormatException e) {

                matched = false;
            }

        } else {

            matched =
                    normalize(extractedValue)
                            .equals(
                                    normalize(referenceValue)
                            );
        }

        ValidationResult result =
                new ValidationResult();

        result.setDocumentId(documentId);
        result.setFieldName(field.getFieldName());
        result.setExtractedValue(extractedValue);
        result.setReferenceValue(referenceValue);

        if (matched) {

            result.setMatchType("EXACT_MATCH");
            result.setValidationStatus("MATCH");
            result.setRiskLevel("LOW");
            result.setReason(
                    "Extracted value matches master record"
            );

        } else {

            result.setMatchType("EXACT_MISMATCH");
            result.setValidationStatus("MISMATCH");
            result.setRiskLevel("HIGH");
            result.setReason(
                    "Extracted value does not match master record"
            );
        }

        return result;
    }

    private String normalize(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }
}