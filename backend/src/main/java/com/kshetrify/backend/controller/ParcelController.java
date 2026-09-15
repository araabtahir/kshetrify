package com.kshetrify.backend.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

    private final JdbcTemplate jdbcTemplate;

    public ParcelController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public Map<String, Object> getParcel(
            @RequestParam String surveyNumber) {

        String geometry = jdbcTemplate.queryForObject(
                """
                SELECT ST_AsGeoJSON(geometry)
                FROM parcels
                WHERE survey_number = ?
                """,
                String.class,
                surveyNumber
        );

        return Map.of(
                "surveyNumber", surveyNumber,
                "geometry", geometry
        );
    }
}