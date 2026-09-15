package com.kshetrify.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kshetrify.backend.entity.Document;
import com.kshetrify.backend.entity.ExtractedField;
import com.kshetrify.backend.repository.DocumentRepository;
import com.kshetrify.backend.repository.ExtractedFieldRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ExtractedFieldRepository extractedFieldRepository;
    private final PythonOcrService pythonOcrService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentService(
            DocumentRepository documentRepository,
            ExtractedFieldRepository extractedFieldRepository,
            PythonOcrService pythonOcrService) {

        this.documentRepository = documentRepository;
        this.extractedFieldRepository = extractedFieldRepository;
        this.pythonOcrService = pythonOcrService;
    }

    public String processDocument(Long documentId) {

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new RuntimeException("Document not found"));

        Path filePath = Paths.get(document.getFilePath());

        String result = pythonOcrService.processDocument(filePath);

        try {
            JsonNode root = objectMapper.readTree(result);
            JsonNode fields = root.get("fields");

            if (fields != null && fields.isObject()) {

                extractedFieldRepository.deleteAll(
                        extractedFieldRepository.findByDocumentId(documentId)
                );

                Iterator<Map.Entry<String, JsonNode>> fieldIterator =
                        fields.fields();

                while (fieldIterator.hasNext()) {

                    Map.Entry<String, JsonNode> entry =
                            fieldIterator.next();

                    String fieldName = entry.getKey();
                    JsonNode fieldData = entry.getValue();

                    JsonNode valueNode = fieldData.get("value");
                    JsonNode confidenceNode =
                            fieldData.get("confidence");

                    if (valueNode == null || valueNode.isNull()) {
                        continue;
                    }

                    ExtractedField extractedField =
                            new ExtractedField();

                    extractedField.setDocumentId(documentId);
                    extractedField.setFieldName(fieldName);
                    extractedField.setExtractedValue(
                            valueNode.asText()
                    );

                    if (confidenceNode != null &&
                            !confidenceNode.isNull()) {

                        extractedField.setConfidence(
                                BigDecimal.valueOf(
                                        confidenceNode.asDouble()
                                )
                        );
                    }

                    extractedFieldRepository.save(
                            extractedField
                    );
                }
            }

            document.setProcessingStatus("OCR_COMPLETED");
            documentRepository.save(document);

            return result;

        } catch (Exception e) {

            document.setProcessingStatus("OCR_FAILED");
            documentRepository.save(document);

            throw new RuntimeException(
                    "Failed to save OCR results: " + e.getMessage(),
                    e
            );
        }
    }
}