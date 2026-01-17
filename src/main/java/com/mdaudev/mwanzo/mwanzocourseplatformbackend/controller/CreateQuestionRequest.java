package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

@lombok.Data
public class CreateQuestionRequest {
    private String questionText;
    private Integer displayOrder;
    private Integer points;
}
