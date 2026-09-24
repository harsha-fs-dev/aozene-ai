package com.dubpilot.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = {"http://localhost:5173", "https://aozene-ai.vercel.app", "https://aozene-oktxti0i7-harsha-d199.vercel.app", "https://aozene-pt8vv9zy3-harsha-d199.vercel.app"})
public class VoiceTokenController {

    @Value("${assemblyai.api.key:}")
    private String assemblyAiApiKey;

    private final RestTemplate restTemplate;

    public VoiceTokenController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Mints a short-lived, single-use AssemblyAI Voice Agent token.
     * The permanent API key never leaves this server.
     */
    @GetMapping("/token")
    public ResponseEntity<?> getVoiceToken() {
        if (assemblyAiApiKey == null || assemblyAiApiKey.isBlank()) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "AssemblyAI API key is not configured on the server. " +
                            "Set the ASSEMBLYAI_API_KEY environment variable."));
        }

        String url = UriComponentsBuilder.fromHttpUrl("https://agents.assemblyai.com/v1/token")
                .queryParam("expires_in_seconds", 120)
                .queryParam("max_session_duration_seconds", 1800)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", assemblyAiApiKey);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (RestClientException e) {
            return ResponseEntity.status(502)
                    .body(Map.of("error", "Failed to obtain a voice session token from AssemblyAI."));
        }
    }
}