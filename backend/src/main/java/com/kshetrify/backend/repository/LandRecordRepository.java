package com.kshetrify.backend.repository;

import com.kshetrify.backend.entity.LandRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LandRecordRepository
        extends JpaRepository<LandRecord, Long> {

    Optional<LandRecord> findByRecordId(String recordId);

    Optional<LandRecord> findBySurveyNumber(String surveyNumber);

    Optional<LandRecord> findByOwnerNameIgnoreCase(String ownerName);
}