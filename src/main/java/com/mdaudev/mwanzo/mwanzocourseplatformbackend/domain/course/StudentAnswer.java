package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Student Answer Entity
 *
 * Records which answer a student selected for each question.
 * Used for grading and review.
 *
 * Database Table: student_answers
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-13
 */
@Entity
@Table(name = "student_answers", indexes = {
        @Index(name = "idx_student_answer_attempt", columnList = "attempt_id"),
        @Index(name = "idx_student_answer_question", columnList = "question_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_attempt_question", columnNames = {"attempt_id", "question_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentAnswer {

    /**
     * Unique identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Quiz attempt this answer belongs to.
     */
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;

    /**
     * Question being answered.
     */
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    /**
     * Answer selected by student.
     */
    @Column(name = "selected_answer_id", nullable = false)
    private UUID selectedAnswerId;

    /**
     * Whether the answer was correct.
     */
    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    /**
     * Points earned for this question.
     */
    @Column(name = "points_earned", nullable = false)
    private Integer pointsEarned;

    @CreationTimestamp
    @Column(name = "answered_at", nullable = false, updatable = false)
    private LocalDateTime answeredAt;
}