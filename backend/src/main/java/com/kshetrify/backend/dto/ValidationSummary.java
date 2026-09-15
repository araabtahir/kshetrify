package com.kshetrify.backend.dto;

import com.kshetrify.backend.entity.ValidationResult;

import java.util.List;

public class ValidationSummary {

    private Long documentId;
    private String overallRisk;
    private String verificationStatus;
    private int totalFields;
    private int matchedFields;
    private int mismatchedFields;
    private List<ValidationResult> results;

    public ValidationSummary() {
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getOverallRisk() {
        return overallRisk;
    }

    public void setOverallRisk(String overallRisk) {
        this.overallRisk = overallRisk;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public int getTotalFields() {
        return totalFields;
    }

    public void setTotalFields(int totalFields) {
        this.totalFields = totalFields;
    }

    public int getMatchedFields() {
        return matchedFields;
    }

    public void setMatchedFields(int matchedFields) {
        this.matchedFields = matchedFields;
    }

    public int getMismatchedFields() {
        return mismatchedFields;
    }

    public void setMismatchedFields(int mismatchedFields) {
        this.mismatchedFields = mismatchedFields;
    }

    public List<ValidationResult> getResults() {
        return results;
    }

    public void setResults(List<ValidationResult> results) {
        this.results = results;
    }
}