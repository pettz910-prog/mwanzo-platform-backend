package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID> {

    @Query("SELECT q FROM Question q WHERE q.quizId = :quizId AND q.isActive = true ORDER BY q.displayOrder ASC")
    List<Question> findActiveQuestionsByQuizId(UUID quizId);

    @Query("SELECT COALESCE(SUM(q.points), 0) FROM Question q WHERE q.quizId = :quizId AND q.isActive = true")
    Integer getTotalPointsByQuizId(UUID quizId);
}
