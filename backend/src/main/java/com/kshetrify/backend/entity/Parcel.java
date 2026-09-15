package com.kshetrify.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "parcels")
public class Parcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "survey_number", unique = true, nullable = false)
    private String surveyNumber;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private String geometry;

    public Parcel() {}

    public Long getId() {
        return id;
    }

    public String getSurveyNumber() {
        return surveyNumber;
    }

    public void setSurveyNumber(String surveyNumber) {
        this.surveyNumber = surveyNumber;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }
}