package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Question Entity
 *
 * Represents individual questions within a quiz.
 * Supports multiple choice questions.
 *
 * Database Table: questions
 *
 * Question Types:
 * - MULTIPLE_CHOICE: Single correct answer
 * - (Future: MULTIPLE_SELECT, TRUE_FALSE, SHORT_ANSWER)
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-13
 */
@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_question_quiz", columnList = "quiz_id"),
        @Index(name = "idx_question_order", columnList = "quiz_id, display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    /**
     * Unique identifier for the question.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Quiz this question belongs to.
     */
    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    /**
     * Question text.
     */
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /**
     * Question type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    @Builder.Default
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    /**
     * Display order within quiz.
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Points awarded for correct answer.
     */
    @Column(name = "points", nullable = false)
    @Builder.Default
    private Integer points = 1;

    /**
     * Explanation shown after answer (optional).
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    /**
     * Image URL for question (optional).
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Whether this question is active.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}