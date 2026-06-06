package com.aiweb.judge.service;

import com.aiweb.judge.dto.CaseRequest;
import com.aiweb.judge.dto.FromJudgeResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class JudgeService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Spring이 GeminiClient를 자동으로 넣어줌 (생성자 주입)
    public JudgeService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public FromJudgeResponse judge(CaseRequest request) {
        String prompt = buildPrompt(request);     // 1) 판사 지시문 만들기
        String aiAnswer = geminiClient.ask(prompt); // 2) AI에게 보내고 답 받기
        return parseAnswer(aiAnswer);              // 3) AI 답(JSON)을 우리 틀에 담기
    }

    // 1) AI에게 줄 지시문 — 여기가 판사의 성격을 정하는 부분
    private String buildPrompt(CaseRequest request) {
        return """
            너는 '추상적 판사'다. 연인이나 친구 사이의 다툼을 객관적으로 판단한다.
            절대 어느 한쪽 편을 들지 말고, 양쪽의 잘못과 입장을 공정하게 본다.
            감정적 위로가 아니라, 제3자가 보는 진짜 상황을 알려주는 것이 목적이다.

            [사건 제목]
            %s

            [%s 의 입장]
            %s

            [%s 의 입장]
            %s

            [추가 맥락/증거]
            %s

            위 내용을 바탕으로 판결하라.
            반드시 아래 JSON 형식으로만 답하라. 다른 설명, 인사말, 코드블록 표시(```)는 절대 붙이지 마라.

            {
              "verdict": "판결문. 누구의 책임이 더 큰지와 그 이유를 3~5문장으로.",
              "faultPercentA": A의 과실 비율 숫자(0~100),
              "faultPercentB": B의 과실 비율 숫자(0~100),
              "objectiveSummary": "한쪽 편 안 든, 제3자가 보는 객관적 상황 요약 2~4문장.",
              "advice": "두 사람의 관계 회복을 위한 현실적 조언 2~3문장."
            }

            주의: faultPercentA 와 faultPercentB 의 합은 반드시 100이어야 한다.
            """.formatted(
                request.title(),
                request.personAName(), request.personAStory(),
                request.personBName(), request.personBStory(),
                request.context()
        );
    }

    // 3) AI가 준 JSON 문자열을 FromJudgeResponse 객체로 변환
    private FromJudgeResponse parseAnswer(String aiAnswer) {
        try {
            // 혹시 AI가 ```json ... ``` 으로 감싸서 주면 그것만 벗겨냄
            String cleaned = aiAnswer
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            return objectMapper.readValue(cleaned, FromJudgeResponse.class);
        } catch (Exception e) {
            // 변환 실패 시 에러 내용을 판결문에 담아 돌려줌 (디버깅용)
            return new FromJudgeResponse(
                    "판결을 정리하지 못했습니다.",
                    50, 50,
                    "AI 원본 응답: " + aiAnswer,
                    "다시 시도해주세요."
            );
        }
    }
}