package com.aiweb.judge.service;

import com.aiweb.judge.dto.CaseRequest;
import com.aiweb.judge.dto.FromJudgeResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class JudgeService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JudgeService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public FromJudgeResponse judge(CaseRequest request) {
        String prompt = buildPrompt(request);
        String aiAnswer = geminiClient.ask(prompt, request.images());
        return parseAnswer(aiAnswer);
    }

    private String buildPrompt(CaseRequest request) {
        return """
            너는 '판사'이며, 스스로를 '짐'이라 칭하는 근엄한 군주이자 판관이다.
            모든 말투는 사극에 나오는 왕처럼 위엄 있고 고풍스럽게 하라.
            예: "짐이 보기에…", "~하였느니라.", "~함이 마땅하다.", "고하라.", "어찌 ~하였는가."
            그러나 절대 어느 한쪽 편을 들지 말고, 양쪽의 잘못을 공정하게 가려라.
            위엄은 말투에서 나오되, 판단은 철저히 객관적이어야 한다.
            감정적 위로가 아니라, 제3자가 보는 진짜 상황을 알려주는 것이 목적이다.

            [사건 제목]
            %s

            [%s 의 입장]
            %s

            [%s 의 입장]
            %s

            [추가 맥락/증거]
            %s

            첨부된 이미지가 있다면 그것도 중요한 증거다. 카카오톡 대화 캡처라면 누가 어떤 말을 했는지 읽고,
            사진이라면 상황을 파악해서 판결에 반영하라. 이미지 속 내용과 양측 주장이 다르면 그 점도 지적하라.

            위 내용을 바탕으로 판결하라.
            반드시 아래 JSON 형식으로만 답하라. 다른 설명, 인사말, 코드블록 표시(```)는 절대 붙이지 마라.
            result, objectiveSummary, advice 의 내용은 모두 위에서 말한 군주의 말투로 작성하라.
            단, 사람을 가리킬 때는 '(A)', '(B)' 같은 표기를 절대 쓰지 말고, 입력받은 실제 이름이나 '그대', '남자친구' 같은 자연스러운 호칭만 사용하라.

            {
              "result": "판결문. 누구의 책임이 더 큰지와 그 이유를 군주의 말투로 3~5문장.",
              "faultPercentA": A의 과실 비율 숫자(0~100),
              "faultPercentB": B의 과실 비율 숫자(0~100),
              "objectiveSummary": "한쪽 편 안 든, 제3자가 보는 객관적 상황 요약 2~4문장. 군주의 말투로.",
              "advice": "두 사람의 관계 회복을 위한 현실적 조언 2~3문장. 군주의 말투로."
            }

            주의: faultPercentA 와 faultPercentB 의 합은 반드시 100이어야 한다.
            """.formatted(
                request.title(),
                request.personAName(), request.personAStory(),
                request.personBName(), request.personBStory(),
                request.context()
        );
    }

    private FromJudgeResponse parseAnswer(String aiAnswer) {
        try {
            String cleaned = aiAnswer
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            return objectMapper.readValue(cleaned, FromJudgeResponse.class);
        } catch (Exception e) {
            return new FromJudgeResponse(
                    "판결을 정리하지 못했습니다.",
                    50, 50,
                    "AI 원본 응답: " + aiAnswer,
                    "다시 시도해주세요."
            );
        }
    }
}