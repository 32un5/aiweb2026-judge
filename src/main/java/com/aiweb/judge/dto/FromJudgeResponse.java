package com.aiweb.judge.dto;

public record FromJudgeResponse (
        String result,
        int faultPercentA,        // A의 과실 비율 (0~100)
        int faultPercentB,        // B의 과실 비율 (0~100)
        String objectiveSummary,
        String advice
) {
}