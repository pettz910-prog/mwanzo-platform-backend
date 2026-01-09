package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CourseDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CourseDetailDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CreateCourseRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.CourseService;
import jakarta.validation.Valid;
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
 * Course REST Controller
 *
 * Handles HTTP requests for course operations.
 * Public endpoints allow browsing without authentication.
 * Instructor/Admin endpoints require authentication (to be added).
 *
 * Base URL: /api/v1/courses
 *
 * Public Endpoints (Student Browsing):
 * - GET    /api/v1/courses                    - Browse all published courses (paginated)
 * - GET    /api/v1/courses/{id}               - Get course details by ID
 * - GET    /api/v1/courses/slug/{slug}        - Get course details by slug
 * - GET    /api/v1/courses/category/{id}      - Get courses by category
 * - GET    /api/v1/courses/search             - Search courses
 * - GET    /api/v1/courses/featured           - Get featured courses
 * - GET    /api/v1/courses/popular            - Get popular courses
 * - GET    /api/v1/courses/free               - Get free courses
 *
 * Instructor Endpoints (to be secured):
 * - POST   /api/v1/courses                    - Create new course
 * - GET    /api/v1/courses/instructor/{id}    - Get instructor's courses
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {

    private final CourseService courseService;

    // ==========================================
    // PUBLIC ENDPOINTS (Student Browsing)
    // ==========================================

    /**
     * Get all published courses with pagination.
     * Main course catalog endpoint.
     *
     * Query Parameters:
     * - page: Page number (default: 0)
     * - size: Page size (default: 12)
     * - sort: Sort field and direction (default: createdAt,desc)
     *
     * Example: /api/v1/courses?page=0&size=12&sort=title,asc
     *
     * @param page Page number (0-indexed)
     * @param size Number of items per page
     * @param sortBy Field to sort by
     * @param direction Sort direction (asc/desc)
     * @return Page of course DTOs
     */
    @GetMapping
    public ResponseEntity<Page<CourseDTO>> getAllCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("GET /api/v1/courses - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, direction);

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CourseDTO> courses = courseService.getAllPublishedCourses(pageable);

        return ResponseEntity.ok(courses);
    }

    /**
     * Get course by ID (detailed view).
     * Returns full course information.
     *
     * @param id Course UUID
     * @return Course detail DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailDTO> getCourseById(@PathVariable UUID id) {
        log.info("GET /api/v1/courses/{} - Fetching course by ID", id);

        CourseDetailDTO course = courseService.getCourseById(id);

        return ResponseEntity.ok(course);
    }

    /**
     * Get course by slug (SEO-friendly URL).
     * Example: /api/v1/courses/slug/digital-marketing-masterclass
     *
     * @param slug Course slug
     * @return Course detail DTO
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CourseDetailDTO> getCourseBySlug(@PathVariable String slug) {
        log.info("GET /api/v1/courses/slug/{} - Fetching course by slug", slug);

        CourseDetailDTO course = courseService.getCourseBySlug(slug);

        return ResponseEntity.ok(course);
    }

    /**
     * Get courses by category with pagination.
     *
     * @param categoryId Category UUID
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param direction Sort direction
     * @return Page of course DTOs
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<CourseDTO>> getCoursesByCategory(
            @PathVariable UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("GET /api/v1/courses/category/{} - page: {}, size: {}", categoryId, page, size);

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CourseDTO> courses = courseService.getCoursesByCategory(categoryId, pageable);

        return ResponseEntity.ok(courses);
    }

    /**
     * Search courses by keyword.
     * Searches in course title and description.
     *
     * Query Parameter:
     * - q: Search keyword (required)
     *
     * Example: /api/v1/courses/search?q=python&page=0&size=12
     *
     * @param keyword Search term
     * @param page Page number
     * @param size Page size
     * @param sortBy Sort field
     * @param direction Sort direction
     * @return Page of matching course DTOs
     */
    @GetMapping("/search")
    public ResponseEntity<Page<CourseDTO>> searchCourses(
            @RequestParam(value = "q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("GET /api/v1/courses/search?q={} - page: {}, size: {}", keyword, page, size);

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CourseDTO> courses = courseService.searchCourses(keyword, pageable);

        return ResponseEntity.ok(courses);
    }

    /**
     * Get featured courses (curated by admin).
     * Limited to top courses selected by platform admins.
     *
     * @param page Page number
     * @param size Page size
     * @return Page of featured course DTOs
     */
    @GetMapping("/featured")
    public ResponseEntity<Page<CourseDTO>> getFeaturedCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {

        log.info("GET /api/v1/courses/featured - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<CourseDTO> courses = courseService.getFeaturedCourses(pageable);

        return ResponseEntity.ok(courses);
    }

    /**
     * Get popular courses (most enrollments).
     * Sorted by enrollment count descending.
     *
     * @param page Page number
     * @param size Page size
     * @return Page of popular course DTOs
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<CourseDTO>> getPopularCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.info("GET /api/v1/courses/popular - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<CourseDTO> courses = courseService.getPopularCourses(pageable);

        return ResponseEntity.ok(courses);
    }

    /**
     * Get free courses (price = 0).
     *
     * @param page Page number
     * @param size Page size
     * @return Page of free course DTOs
     */
    @GetMapping("/free")
    public ResponseEntity<Page<CourseDTO>> getFreeCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.info("GET /api/v1/courses/free - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<CourseDTO> courses = courseService.getFreeCourses(pageable);

        return ResponseEntity.ok(courses);
    }

    // ==========================================
    // INSTRUCTOR ENDPOINTS (To be secured)
    // ==========================================

    /**
     * Create a new course (Instructor only).
     * TODO: Add @PreAuthorize("hasRole('INSTRUCTOR')") when security is implemented
     * TODO: Extract instructorId from JWT token instead of request param
     *
     * @param request Course creation request with validation
     * @param instructorId Temporary: Instructor UUID (will come from JWT later)
     * @return Created course detail DTO
     */
    @PostMapping
    public ResponseEntity<CourseDetailDTO> createCourse(
            @Valid @RequestBody CreateCourseRequest request,
            @RequestParam UUID instructorId) {  // TEMPORARY - will come from JWT token

        log.info("POST /api/v1/courses - Creating course: {} by instructor: {}",
                request.getTitle(), instructorId);

        CourseDetailDTO course = courseService.createCourse(request, instructorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(course);
    }

    /**
     * Get courses by instructor.
     * Shows all courses (any status) for the instructor.
     *
     * @param instructorId Instructor UUID
     * @param page Page number
     * @param size Page size
     * @return Page of instructor's course DTOs
     */
    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<Page<CourseDTO>> getCoursesByInstructor(
            @PathVariable UUID instructorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        log.info("GET /api/v1/courses/instructor/{} - page: {}, size: {}", instructorId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<CourseDTO> courses = courseService.getCoursesByInstructor(instructorId, pageable);

        return ResponseEntity.ok(courses);
    }
}