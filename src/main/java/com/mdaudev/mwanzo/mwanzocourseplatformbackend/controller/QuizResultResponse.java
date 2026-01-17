package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.QuizAttempt;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.StudentAnswer;

import java.util.List;

@lombok.Data
@lombok.AllArgsConstructor
public class QuizResultResponse {
    private QuizAttempt attempt;
    private Boolean showCorrectAnswers;
    private List<StudentAnswer> studentAnswers;
}
