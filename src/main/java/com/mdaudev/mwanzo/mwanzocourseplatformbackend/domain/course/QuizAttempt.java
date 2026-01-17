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
 * Quiz Attempt Entity
 *
 * Represents a student's attempt to complete a quiz.
 * Tracks score, answers, and completion status.
 *
 * Database Table: quiz_attempts
 *
 * Business Rules:
 * - One attempt per quiz per student (until submitted)
 * - Score calculated upon submission
 * - Student must score >= passing_score to pass
 * - Attempt limit enforced if quiz has max_attempts
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-13
 */
@Entity
@Table(name = "quiz_attempts", indexes = {
        @Index(name = "idx_attempt_student", columnList = "student_id"),
        @Index(name = "idx_attempt_quiz", columnList = "quiz_id"),
        @Index(name = "idx_attempt_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_attempt_student_quiz", columnList = "student_id, quiz_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizAttempt {

    /**
     * Unique identifier for the attempt.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Student taking the quiz.
     */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /**
     * Enrollment this attempt belongs to.
     */
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    /**
     * Quiz being attempted.
     */
    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    /**
     * Course this quiz belongs to.
     */
    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    /**
     * Attempt number (1st attempt, 2nd attempt, etc.).
     */
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    /**
     * Score achieved (0-100 percentage).
     */
    @Column(name = "score")
    private Integer score;

    /**
     * Points earned.
     */
    @Column(name = "points_earned")
    private Integer pointsEarned;

    /**
     * Total points possible.
     */
    @Column(name = "total_points")
    private Integer totalPoints;

    /**
     * Number of correct answers.
     */
    @Column(name = "correct_answers")
    private Integer correctAnswers;

    /**
     * Total number of questions.
     */
    @Column(name = "total_questions")
    private Integer totalQuestions;

    /**
     * Whether the quiz was passed.
     */
    @Column(name = "is_passed")
    private Boolean isPassed;

    /**
     * Attempt status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    /**
     * When the attempt was started.
     */
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    /**
     * When the attempt was submitted.
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * Time taken in seconds.
     */
    @Column(name = "time_taken_seconds")
    private Integer timeTakenSeconds;

    /**
     * IP address of student (anti-cheat).
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * User agent (browser info, anti-cheat).
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Submit the attempt and calculate score.
     */
    public void submit(Integer correctAnswers, Integer totalQuestions,
                       Integer pointsEarned, Integer totalPoints,
                       Integer passingScore) {
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.pointsEarned = pointsEarned;
        this.totalPoints = totalPoints;
        this.score = totalPoints > 0 ? (int) ((pointsEarned * 100.0) / totalPoints) : 0;
        this.isPassed = this.score >= passingScore;
        this.status = AttemptStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.timeTakenSeconds = (int) java.time.Duration.between(startedAt, submittedAt).getSeconds();
    }

    /**
     * Check if attempt is still in progress.
     */
    public boolean isInProgress() {
        return status == AttemptStatus.IN_PROGRESS;
    }

    /**
     * Check if attempt has timed out.
     */
    public boolean hasTimedOut(Integer timeLimitMinutes) {
        if (timeLimitMinutes == null || timeLimitMinutes <= 0) return false;

        LocalDateTime deadline = startedAt.plusMinutes(timeLimitMinutes);
        return LocalDateTime.now().isAfter(deadline) && isInProgress();
    }
}