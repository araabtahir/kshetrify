package com.kshetrify.backend.controller;

import com.kshetrify.backend.entity.LandRecord;
import com.kshetrify.backend.repository.LandRecordRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/land-records")
public class LandRecordController {

    private final LandRecordRepository repository;

    public LandRecordController(LandRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<LandRecord> getAllLandRecords() {
        return repository.findAll();
    }
}