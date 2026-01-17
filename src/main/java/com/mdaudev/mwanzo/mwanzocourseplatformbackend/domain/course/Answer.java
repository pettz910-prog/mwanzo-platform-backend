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
 * Answer Entity
 *
 * Represents possible answers for a quiz question.
 * Supports multiple choice and other auto-gradable question types.
 *
 * Database Table: answers
 *
 * @author Mwanzo
 * @version 1.0
 * @since 2026-01-14
 */
@Entity
@Table(name = "answers", indexes = {
        @Index(name = "idx_answer_question", columnList = "question_id"),
        @Index(name = "idx_answer_order", columnList = "question_id, display_order")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    /**
     * Unique identifier for the answer.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Question this answer belongs to.
     */
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    /**
     * Text of the answer.
     */
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;

    /**
     * Whether this answer is correct.
     */
    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    /**
     * Display order within the question.
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    /**
     * Optional image URL for this answer.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
