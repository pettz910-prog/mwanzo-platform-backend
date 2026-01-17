package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import java.util.UUID;

@lombok.Data
public class CreateQuizRequest {
    private UUID courseId;
    private UUID sectionId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private Integer maxAttempts;
    private Integer displayOrder;
}
