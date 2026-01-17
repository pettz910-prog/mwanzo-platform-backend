package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    @Query("SELECT q FROM Quiz q WHERE q.courseId = :courseId AND q.isPublished = true ORDER BY q.displayOrder ASC")
    List<Quiz> findPublishedQuizzesByCourseId(UUID courseId);

    @Query("SELECT q FROM Quiz q WHERE q.courseId = :courseId AND q.isRequired = true")
    List<Quiz> findRequiredQuizzesByCourseId(UUID courseId);
}
