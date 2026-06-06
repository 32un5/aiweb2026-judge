package com.aiweb.judge.dto;

public record CaseRequest(
        String title,        // 사건 제목 (예: "데이트 약속 펑크 사건")
        String personAName,  // A의 이름 (예: "나")
        String personAStory, // A의 입장/주장
        String personBName,  // B의 이름 (예: "상대방")
        String personBStory, // B의 입장/주장
        String context       // 추가 증거·맥락 (카톡 내용, 상황 설명 등)
) {
}