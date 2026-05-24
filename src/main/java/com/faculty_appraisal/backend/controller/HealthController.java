package com.faculty_appraisal.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> health() {
        return Map.of(
                "message", "Faculty Appraisal API is running!",
                "status", "online",
                "version", "2.0.0"
        );
    }
}
