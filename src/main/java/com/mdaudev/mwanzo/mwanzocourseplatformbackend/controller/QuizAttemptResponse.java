package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Quiz;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.QuizAttempt;

import java.util.List;

@lombok.Data
@lombok.AllArgsConstructor
public class QuizAttemptResponse {
    private QuizAttempt attempt;
    private Quiz quiz;
    private List<QuestionWithAnswers> questions;
}
