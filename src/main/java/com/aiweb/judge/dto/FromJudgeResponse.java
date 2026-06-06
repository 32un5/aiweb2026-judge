package com.aiweb.judge.dto;

public record FromJudgeResponse (
        String result,           // 판결문 (예: "쌍방 과실이나 A의 책임이 더 큽니다")
        int faultPercentA,        // A의 과실 비율 (0~100)
        int faultPercentB,        // B의 과실 비율 (0~100)
        String objectiveSummary,  // 한쪽 편 안 든 객관적 상황 요약
        String advice             // 화해/개선을 위한 조언
) {
}