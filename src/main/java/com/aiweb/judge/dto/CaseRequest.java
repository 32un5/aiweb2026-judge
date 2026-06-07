package com.aiweb.judge.dto;

import java.util.List;

public record CaseRequest(
        String title,
        String personAName,
        String personAStory,
        String personBName,
        String personBStory,
        String context,
        List<EvidenceImage> images
) {

    public record EvidenceImage(String mimeType, String data) {
    }

}