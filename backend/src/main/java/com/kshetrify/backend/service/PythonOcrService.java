package com.kshetrify.backend.service;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.MultipartBodyBuilder;

import java.nio.file.Path;

@Service
public class PythonOcrService {

    private final RestClient restClient;

    public PythonOcrService() {
        this.restClient = RestClient.builder()
                .baseUrl(System.getenv().getOrDefault(
    "AI_SERVICE_URL",
    "http://localhost:8000"
))
                .build();
    }

    public String processDocument(Path filePath) {

        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("file", new FileSystemResource(filePath));

        return restClient.post()
                .uri("/ocr")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(String.class);
    }
}