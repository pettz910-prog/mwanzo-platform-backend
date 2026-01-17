package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.EnrollmentDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.EnrollmentService;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Enrollment REST Controller
 *
 * Handles student course enrollments and progress tracking.
 *
 * Base URL: /api/v1/enrollments
 *
 * Endpoints:
 * - POST   /api/v1/enrollments/{courseId}      - Enroll in course
 * - GET    /api/v1/enrollments                 - Get my enrollments
 * - GET    /api/v1/enrollments/{id}            - Get enrollment details
 * - GET    /api/v1/enrollments/check/{courseId} - Check if enrolled
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final JwtService jwtService;

    /**
     * Enroll in a course.
     * For free courses: instantly activates enrollment.
     * For paid courses: creates PENDING_PAYMENT enrollment (user must complete payment).
     *
     * TODO: Extract studentId from JWT token (SecurityContext) instead of param
     *
     * @param courseId Course UUID
     * @param studentId Student UUID (temporary - will come from JWT)
     * @return Created enrollment DTO
     */
    @PostMapping("/{courseId}")
    public ResponseEntity<EnrollmentDTO> enrollInCourse(
            @PathVariable UUID courseId,
            @RequestParam UUID studentId) {  // TEMPORARY - will come from JWT token

        log.info("POST /api/v1/enrollments/{} - Enrolling student {}", courseId, studentId);

        EnrollmentDTO enrollment = enrollmentService.enrollInCourse(studentId, courseId);

        return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
    }

    /**
     * Get all my enrollments (student's enrolled courses).
     *
     * @param studentId Student UUID (temporary)
     * @param page Page number
     * @param size Page size
     * @return Page of enrollment DTOs
     */
    @GetMapping
    public ResponseEntity<Page<EnrollmentDTO>> getMyEnrollments(
            @RequestParam UUID studentId,  // TEMPORARY
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.info("GET /api/v1/enrollments - Fetching enrollments for student {}", studentId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<EnrollmentDTO> enrollments = enrollmentService.getStudentEnrollments(studentId, pageable);

        return ResponseEntity.ok(enrollments);
    }

    /**
     * Get active enrollments (My Courses page).
     * Only returns ACTIVE and COMPLETED enrollments.
     *
     * @param studentId Student UUID (temporary)
     * @param page Page number
     * @param size Page size
     * @return Page of active enrollment DTOs
     */
    @GetMapping("/active")
    public ResponseEntity<Page<EnrollmentDTO>> getActiveCourses(
            @RequestParam UUID studentId,  // TEMPORARY
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.info("GET /api/v1/enrollments/active - Fetching active enrollments for student {}", studentId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("lastAccessedAt").descending());
        Page<EnrollmentDTO> enrollments = enrollmentService.getActiveEnrollments(studentId, pageable);

        return ResponseEntity.ok(enrollments);
    }

    /**
     * Get enrollment details by ID.
     *
     * @param enrollmentId Enrollment UUID
     * @return Enrollment DTO
     */
    @GetMapping("/{enrollmentId}")
    public ResponseEntity<EnrollmentDTO> getEnrollmentById(@PathVariable UUID enrollmentId) {
        log.info("GET /api/v1/enrollments/{} - Fetching enrollment", enrollmentId);

        EnrollmentDTO enrollment = enrollmentService.getEnrollmentById(enrollmentId);

        return ResponseEntity.ok(enrollment);
    }

    /**
     * Check if student is enrolled in a course.
     * Used to show "Continue Learning" vs "Enroll Now" button.
     *
     * @param courseId Course UUID
     * @param studentId Student UUID (temporary)
     * @return Response with isEnrolled and hasAccess flags
     */
    @GetMapping("/check/{courseId}")
    public ResponseEntity<EnrollmentCheckResponse> checkEnrollment(
            @PathVariable UUID courseId,
            @RequestParam UUID studentId) {  // TEMPORARY

        log.info("GET /api/v1/enrollments/check/{} - Checking enrollment for student {}",
                courseId, studentId);

        boolean isEnrolled = enrollmentService.isEnrolled(studentId, courseId);
        boolean hasAccess = enrollmentService.hasAccess(studentId, courseId);

        EnrollmentCheckResponse response = EnrollmentCheckResponse.builder()
                .isEnrolled(isEnrolled)
                .hasAccess(hasAccess)
                .build();

        return ResponseEntity.ok(response);
    }
}

/**
 * Enrollment Check Response DTO
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
class EnrollmentCheckResponse {
    /**
     * Whether student is enrolled (any status).
     */
    private Boolean isEnrolled;

    /**
     * Whether student can access course content (ACTIVE or COMPLETED).
     */
    private Boolean hasAccess;
}