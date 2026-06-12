package com.aiweb.judge.service;

import com.aiweb.judge.dto.AutoCaseResponse;
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

            [이 사연을 올린 사람]
            %s

            첨부된 이미지가 있다면 그것도 중요한 증거다. 카카오톡 대화 캡처라면 누가 어떤 말을 했는지 읽고,
            사진이라면 상황을 파악해서 판결에 반영하라. 이미지 속 내용과 양측 주장이 다르면 그 점도 지적하라.

            위 내용을 바탕으로 판결하라.
            반드시 아래 JSON 형식으로만 답하라. 다른 설명, 인사말, 코드블록 표시(```)는 절대 붙이지 마라.
            result, objectiveSummary, advice, empathy 의 내용은 모두 위에서 말한 군주의 말투로 작성하라.
            단, 사람을 가리킬 때는 '(A)', '(B)' 같은 표기를 절대 쓰지 말고, 입력받은 실제 이름이나 '그대', '남자친구' 같은 자연스러운 호칭만 사용하라.

            {
              "result": "판결문. 누구의 책임이 더 큰지와 그 이유를 군주의 말투로 3~5문장.",
              "faultPercentA": A의 과실 비율 숫자(0~100),
              "faultPercentB": B의 과실 비율 숫자(0~100),
              "objectiveSummary": "한쪽 편 안 든, 제3자가 보는 객관적 상황 요약 2~4문장. 군주의 말투로.",
              "advice": "두 사람의 관계 회복을 위한 현실적 조언 2~3문장. 군주의 말투로.",
              "empathy": "위 '이 사연을 올린 사람'의 입장에서 그 마음을 헤아리고 다독이는 말. 판결은 객관적이었으나, 올린 사람의 답답함과 속상함에 공감해주는 따뜻한 말 2~3문장. 군주의 말투로. 올린 사람 정보가 없으면 빈 문자열."
            }

            주의: faultPercentA 와 faultPercentB 의 합은 반드시 100이어야 한다.
            """.formatted(
                request.title(),
                request.personAName(), request.personAStory(),
                request.personBName(), request.personBStory(),
                request.context(),
                request.myName() == null ? "(밝히지 않음)" : request.myName()
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
                    "다시 시도해주세요.",
                    ""
            );
        }
    }

    public AutoCaseResponse extract(String chatText, java.util.List<com.aiweb.judge.dto.CaseRequest.EvidenceImage> images) {
        String prompt = buildExtractPrompt(chatText);
        String aiAnswer = geminiClient.ask(prompt, images);
        return parseExtract(aiAnswer);
    }

    private String buildExtractPrompt(String chatText) {
        return """
            너는 대화 분석가다. 아래는 두 사람이 다툰 카카오톡 대화다.
            (텍스트가 비어 있으면 첨부된 이미지가 그 대화 캡처다.)
            이 대화를 읽고 다음을 추출하라.

            - title: 이 다툼을 한 줄로 요약한 사건 제목 (예: "데이트 약속 펑크 사건")
            - personAName: 대화에 나오는 첫 번째 사람의 이름(또는 화자명)
            - personAStory: A의 입장과 주장을 대화에 근거해 2~4문장으로 정리
            - personBName: 두 번째 사람의 이름(또는 화자명)
            - personBStory: B의 입장과 주장을 대화에 근거해 2~4문장으로 정리

            [카카오톡 대화]
            %s

            반드시 아래 JSON 형식으로만 답하라. 다른 설명, 코드블록 표시(```)는 절대 붙이지 마라.
            {
              "title": "...",
              "personAName": "...",
              "personAStory": "...",
              "personBName": "...",
              "personBStory": "..."
            }
            """.formatted(chatText == null ? "" : chatText);
    }

    private AutoCaseResponse parseExtract(String aiAnswer) {
        try {
            String cleaned = aiAnswer.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(cleaned, AutoCaseResponse.class);
        } catch (Exception e) {
            return new AutoCaseResponse("", "", "", "", "");
        }
    }

}