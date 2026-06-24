package com.aiweb.judge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GeminiClient {

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    private final RestClient restClient = RestClient.create();

    public String ask(String prompt, java.util.List<com.aiweb.judge.dto.CaseRequest.EvidenceImage> images) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        java.util.List<Object> parts = new java.util.ArrayList<>();
        parts.add(java.util.Map.of("text", prompt));

        if (images != null) {
            for (var img : images) {
                parts.add(java.util.Map.of("inline_data", java.util.Map.of("mime_type", img.mimeType(), "data", img.data())));
            }
        }

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", parts.toArray())
                }
        );

        final int MAX_ATTEMPTS = 3;
        final long TIME_BUDGET_MS = 25000;
        long start = System.currentTimeMillis();
        org.springframework.web.client.HttpServerErrorException last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Map<?, ?> response = restClient.post().uri(url).header("Content-Type", "application/json").body(body).retrieve().body(Map.class);
                return extractText(response);
            } catch (org.springframework.web.client.HttpServerErrorException e) {
                last = e;
                long elapsed = System.currentTimeMillis() - start;
                if (attempt == MAX_ATTEMPTS || elapsed > TIME_BUDGET_MS) break;
                try {
                    Thread.sleep(1500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last;
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        try {
            var candidates = (java.util.List<Map<String, Object>>) response.get("candidates");
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (java.util.List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "AI 응답을 읽지 못했습니다: " + response;
        }
    }
}