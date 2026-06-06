package com.aiweb.judge.dto;

public record CaseRequest(
        String title,
        String personAName,
        String personAStory,
        String personBName,
        String personBStory,
        String context
) {
}