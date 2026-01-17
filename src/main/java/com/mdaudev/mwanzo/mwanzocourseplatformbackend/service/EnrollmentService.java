package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Course;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Enrollment;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.EnrollmentStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.EnrollmentDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CourseRepository;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Enrollment Service
 *
 * Business logic for course enrollments.
 * Handles enrollment creation, payment activation, and progress tracking.
 *
 * Microservices-Ready:
 * - Uses UUID references (no tight coupling to Course entity)
 * - Can be extracted to "Enrollment Service" independently
 * - Fetches course data via CourseService (service-to-service call)
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;

    /**
     * Enroll student in a course (free courses only for now).
     * For paid courses, creates PENDING_PAYMENT enrollment.
     *
     * @param studentId Student UUID
     * @param courseId Course UUID
     * @return Enrollment DTO
     * @throws IllegalArgumentException if already enrolled or course not found
     */
    @Transactional
    public EnrollmentDTO enrollInCourse(UUID studentId, UUID courseId) {
        log.info("Enrolling student {} in course {}", studentId, courseId);

        // Check if already enrolled
        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            log.error("Student {} is already enrolled in course {}", studentId, courseId);
            throw new IllegalArgumentException("You are already enrolled in this course");
        }

        // Get course details
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // Check if course is published
        if (!course.getIsPublished()) {
            throw new IllegalArgumentException("This course is not available for enrollment");
        }

        // Determine enrollment status based on price
        EnrollmentStatus status = course.isFree()
                ? EnrollmentStatus.ACTIVE
                : EnrollmentStatus.PENDING_PAYMENT;

        // Create enrollment
        Enrollment enrollment = Enrollment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .pricePaid(course.getPrice())
                .status(status)
                .progressPercentage(0)
                .videosCompleted(false)
                .quizzesCompleted(false)
                .isCompleted(false)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        // If free course, increment enrollment count
        if (course.isFree()) {
            course.incrementEnrollmentCount();
            courseRepository.save(course);
        }

        log.info("Enrollment created with ID: {} and status: {}",
                savedEnrollment.getId(), savedEnrollment.getStatus());

        return mapToDTO(savedEnrollment, course);
    }

    /**
     * Activate enrollment after successful payment.
     * Called by payment webhook/callback.
     *
     * @param enrollmentId Enrollment UUID
     * @param paymentId Payment transaction ID
     * @return Activated enrollment DTO
     */
    @Transactional
    public EnrollmentDTO activateEnrollment(UUID enrollmentId, UUID paymentId) {
        log.info("Activating enrollment {} with payment {}", enrollmentId, paymentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        // Activate enrollment
        enrollment.activate(paymentId);
        enrollmentRepository.save(enrollment);

        // Increment course enrollment count
        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", enrollment.getCourseId()));

        course.incrementEnrollmentCount();
        courseRepository.save(course);

        log.info("Enrollment {} activated successfully", enrollmentId);

        return mapToDTO(enrollment, course);
    }

    /**
     * Get all enrollments for a student.
     *
     * @param studentId Student UUID
     * @param pageable Pagination parameters
     * @return Page of enrollment DTOs
     */
    public Page<EnrollmentDTO> getStudentEnrollments(UUID studentId, Pageable pageable) {
        log.debug("Fetching enrollments for student {}", studentId);

        Page<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId, pageable);

        return enrollments.map(enrollment -> {
            Course course = courseRepository.findById(enrollment.getCourseId())
                    .orElse(null);
            return mapToDTO(enrollment, course);
        });
    }

    /**
     * Get active enrollments for a student (for "My Courses" page).
     *
     * @param studentId Student UUID
     * @param pageable Pagination parameters
     * @return Page of active enrollment DTOs
     */
    public Page<EnrollmentDTO> getActiveEnrollments(UUID studentId, Pageable pageable) {
        log.debug("Fetching active enrollments for student {}", studentId);

        Page<Enrollment> enrollments = enrollmentRepository
                .findActiveEnrollmentsByStudentId(studentId, pageable);

        return enrollments.map(enrollment -> {
            Course course = courseRepository.findById(enrollment.getCourseId())
                    .orElse(null);
            return mapToDTO(enrollment, course);
        });
    }

    /**
     * Get enrollment by ID.
     *
     * @param enrollmentId Enrollment UUID
     * @return Enrollment DTO
     */
    public EnrollmentDTO getEnrollmentById(UUID enrollmentId) {
        log.debug("Fetching enrollment {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElse(null);

        return mapToDTO(enrollment, course);
    }

    /**
     * Check if student is enrolled in a course.
     *
     * @param studentId Student UUID
     * @param courseId Course UUID
     * @return true if enrolled (any status)
     */
    public boolean isEnrolled(UUID studentId, UUID courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Check if student has access to course content.
     * Only ACTIVE or COMPLETED enrollments allow access.
     *
     * @param studentId Student UUID
     * @param courseId Course UUID
     * @return true if student can access course
     */
    public boolean hasAccess(UUID studentId, UUID courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(Enrollment::isAccessible)
                .orElse(false);
    }

    /**
     * Update enrollment progress.
     *
     * @param enrollmentId Enrollment UUID
     * @param progressPercentage New progress percentage
     */
    @Transactional
    public void updateProgress(UUID enrollmentId, Integer progressPercentage) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        enrollment.updateProgress(progressPercentage);
        enrollmentRepository.save(enrollment);
    }

    /**
     * Map Enrollment entity to EnrollmentDTO.
     *
     * @param enrollment Enrollment entity
     * @param course Course entity (can be null)
     * @return Enrollment DTO
     */
    private EnrollmentDTO mapToDTO(Enrollment enrollment, Course course) {
        EnrollmentDTO.EnrollmentDTOBuilder builder = EnrollmentDTO.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .courseId(enrollment.getCourseId())
                .pricePaid(enrollment.getPricePaid())
                .status(enrollment.getStatus().name())
                .progressPercentage(enrollment.getProgressPercentage())
                .videosCompleted(enrollment.getVideosCompleted())
                .quizzesCompleted(enrollment.getQuizzesCompleted())
                .isCompleted(enrollment.getIsCompleted())
                .completedAt(enrollment.getCompletedAt())
                .certificateId(enrollment.getCertificateId())
                .lastAccessedAt(enrollment.getLastAccessedAt())
                .expiresAt(enrollment.getExpiresAt())
                .createdAt(enrollment.getCreatedAt());

        // Add course details if available
        if (course != null) {
            builder.courseTitle(course.getTitle())
                    .courseSlug(course.getSlug())
                    .courseThumbnailUrl(course.getThumbnailUrl())
                    .instructorName("Instructor Name"); // TODO: Fetch from User Service
        }

        return builder.build();
    }
}