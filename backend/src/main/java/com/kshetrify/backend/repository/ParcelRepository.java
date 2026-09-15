package com.kshetrify.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParcelRepository extends JpaRepository<com.kshetrify.backend.entity.Parcel, Long> {

    @Query(value = """
        SELECT ST_AsGeoJSON(geometry)
        FROM parcels
        WHERE survey_number = :surveyNumber
        """, nativeQuery = true)
    String findGeometryAsGeoJson(
            @Param("surveyNumber") String surveyNumber
    );
}