package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import java.util.UUID;

@lombok.Data
public class AnswerQuestionRequest {
    private UUID attemptId;
    private UUID selectedAnswerId;
}
