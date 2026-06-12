package com.aiweb.judge.dto;

public record FromJudgeResponse (
        String result,
        int faultPercentA,
        int faultPercentB,
        String objectiveSummary,
        String advice,
        String empathy
) {
}