package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Student Answer Repository
 */
@Repository
public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, UUID> {

    List<StudentAnswer> findByAttemptId(UUID attemptId);

    Optional<StudentAnswer> findByAttemptIdAndQuestionId(UUID attemptId, UUID questionId);

    long countByAttemptId(UUID attemptId);
}