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

    public String ask(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        Map<?, ?> response = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        return extractText(response);
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