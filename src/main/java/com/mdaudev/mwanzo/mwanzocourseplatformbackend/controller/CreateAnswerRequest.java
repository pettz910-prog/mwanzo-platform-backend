package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

@lombok.Data
public class CreateAnswerRequest {
    private String answerText;
    private Boolean isCorrect;
    private Integer displayOrder;
}
