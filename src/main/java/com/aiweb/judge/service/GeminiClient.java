package com.aiweb.judge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class GeminiClient {

    // application.properties의 값을 자동으로 꺼내 넣어줌
    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    private final RestClient restClient = RestClient.create();

    /**
     * 프롬프트(질문)를 Gemini에 보내고, AI가 쓴 텍스트 답변을 돌려준다.
     */
    public String ask(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        // Gemini가 요구하는 요청 형태 (JSON 구조)
        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", prompt)
                        })
                }
        );

        // 요청 보내고 응답을 Map으로 받음
        Map<?, ?> response = restClient.post()
                .uri(url)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        // 응답 JSON에서 실제 답변 텍스트만 꺼냄
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