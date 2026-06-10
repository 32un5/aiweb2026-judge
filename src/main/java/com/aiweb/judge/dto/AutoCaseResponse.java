package com.aiweb.judge.dto;

public record AutoCaseResponse(
        String title,
        String personAName,
        String personAStory,
        String personBName,
        String personBStory
) {
}