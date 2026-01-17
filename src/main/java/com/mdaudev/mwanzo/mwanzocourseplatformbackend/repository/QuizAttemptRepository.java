package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.AttemptStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Quiz Attempt Repository
 */
@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, UUID> {

    List<QuizAttempt> findByStudentIdAndQuizIdOrderByAttemptNumberDesc(UUID studentId, UUID quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.studentId = :studentId AND qa.quizId = :quizId AND qa.status = :status")
    Optional<QuizAttempt> findInProgressAttempt(@Param("studentId") UUID studentId,
                                                @Param("quizId") UUID quizId,
                                                @Param("status") AttemptStatus status);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.studentId = :studentId AND qa.quizId = :quizId")
    long countAttemptsByStudentAndQuiz(@Param("studentId") UUID studentId, @Param("quizId") UUID quizId);

    @Query("SELECT qa FROM QuizAttempt qa WHERE qa.studentId = :studentId AND qa.quizId = :quizId AND qa.isPassed = true ORDER BY qa.score DESC")
    Optional<QuizAttempt> findBestAttempt(@Param("studentId") UUID studentId, @Param("quizId") UUID quizId);

    List<QuizAttempt> findByEnrollmentIdOrderByCreatedAtDesc(UUID enrollmentId);

    @Query("SELECT COUNT(qa) FROM QuizAttempt qa WHERE qa.enrollmentId = :enrollmentId AND qa.isPassed = true")
    long countPassedQuizzesByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);
}