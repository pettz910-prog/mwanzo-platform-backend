package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Section Repository
 */
@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findByCourseIdOrderByDisplayOrderAsc(UUID courseId);

    @Query("SELECT s FROM Section s WHERE s.courseId = :courseId AND s.isPublished = true ORDER BY s.displayOrder ASC")
    List<Section> findPublishedSectionsByCourseId(@Param("courseId") UUID courseId);

    long countByCourseId(UUID courseId);
}