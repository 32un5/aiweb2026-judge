package com.aiweb.judge.controller;

import com.aiweb.judge.dto.CaseRequest;
import com.aiweb.judge.dto.FromJudgeResponse;
import com.aiweb.judge.service.JudgeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JudgeController {

    private final JudgeService judgeService;

    public JudgeController(JudgeService judgeService) {
        this.judgeService = judgeService;
    }

    @PostMapping("/api/judge")
    public FromJudgeResponse judge(@RequestBody CaseRequest request) {
        return judgeService.judge(request);
    }
}