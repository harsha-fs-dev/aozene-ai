package com.dubpilot.backend.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "https://aozene-ai.vercel.app", "https://aozene-oktxti0i7-harsha-d199.vercel.app"})
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {

        Map<String, String> response = new LinkedHashMap<>();

        response.put("status", "UP");
        response.put("service", "DubPilot AI Backend");

        return response;
    }
}