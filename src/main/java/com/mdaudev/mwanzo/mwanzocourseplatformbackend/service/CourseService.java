package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Category;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Course;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.CourseLevel;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.CourseStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CategoryDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CourseDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CourseDetailDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CreateCourseRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CategoryRepository;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Course Service
 *
 * Business logic layer for course operations.
 * Handles course CRUD, search, filtering, and DTO mapping.
 *
 * Microservices-Ready Design:
 * - Uses UUID for instructor (not direct User entity reference)
 * - All JSON serialization/deserialization for complex fields
 * - Can be extracted to "Course Service" independently
 * - Category relationship handled via UUID when split
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper;

    // ==========================================
    // PUBLIC COURSE BROWSING (Students)
    // ==========================================

    /**
     * Get all published courses with pagination.
     * Main course catalog endpoint.
     *
     * @param pageable Pagination parameters
     * @return Page of course DTOs
     */
    public Page<CourseDTO> getAllPublishedCourses(Pageable pageable) {
        log.debug("Fetching all published courses, page: {}", pageable.getPageNumber());

        Page<Course> courses = courseRepository.findByIsPublishedTrue(pageable);

        log.info("Found {} published courses", courses.getTotalElements());

        return courses.map(this::mapToListDTO);
    }

    /**
     * Get course by ID (detailed view).
     * Returns full course information.
     *
     * @param id Course UUID
     * @return Course detail DTO
     * @throws RuntimeException if course not found
     */
    public CourseDetailDTO getCourseById(UUID id) {
        log.debug("Fetching course by ID: {}", id);

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Course not found with ID: {}", id);
                    return new RuntimeException("Course not found with ID: " + id);
                });

        log.info("Found course: {}", course.getTitle());

        return mapToDetailDTO(course);
    }

    /**
     * Get course by slug (SEO-friendly URL).
     *
     * @param slug Course slug
     * @return Course detail DTO
     * @throws RuntimeException if course not found
     */
    public CourseDetailDTO getCourseBySlug(String slug) {
        log.debug("Fetching course by slug: {}", slug);

        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() -> {
                    log.error("Course not found with slug: {}", slug);
                    return new RuntimeException("Course not found with slug: " + slug);
                });

        log.info("Found course: {}", course.getTitle());

        return mapToDetailDTO(course);
    }

    /**
     * Get courses by category with pagination.
     *
     * @param categoryId Category UUID
     * @param pageable Pagination parameters
     * @return Page of course DTOs
     */
    public Page<CourseDTO> getCoursesByCategory(UUID categoryId, Pageable pageable) {
        log.debug("Fetching courses for category: {}", categoryId);

        // Verify category exists
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + categoryId));

        Page<Course> courses = courseRepository.findByCategoryIdAndIsPublishedTrue(categoryId, pageable);

        log.info("Found {} courses in category", courses.getTotalElements());

        return courses.map(this::mapToListDTO);
    }

    /**
     * Search courses by keyword (title or description).
     *
     * @param keyword Search term
     * @param pageable Pagination parameters
     * @return Page of matching course DTOs
     */
    public Page<CourseDTO> searchCourses(String keyword, Pageable pageable) {
        log.debug("Searching courses with keyword: {}", keyword);

        Page<Course> courses = courseRepository.searchPublishedCourses(keyword, pageable);

        log.info("Found {} courses matching keyword: {}", courses.getTotalElements(), keyword);

        return courses.map(this::mapToListDTO);
    }

    /**
     * Get featured courses (curated by admin).
     *
     * @param pageable Pagination parameters
     * @return Page of featured course DTOs
     */
    public Page<CourseDTO> getFeaturedCourses(Pageable pageable) {
        log.debug("Fetching featured courses");

        Page<Course> courses = courseRepository.findByIsFeaturedTrueAndIsPublishedTrue(pageable);

        log.info("Found {} featured courses", courses.getTotalElements());

        return courses.map(this::mapToListDTO);
    }

    /**
     * Get popular courses (most enrollments).
     *
     * @param pageable Pagination parameters
     * @return Page of popular course DTOs
     */
    public Page<CourseDTO> getPopularCourses(Pageable pageable) {
        log.debug("Fetching popular courses");

        Page<Course> courses = courseRepository.findPopularCourses(pageable);

        log.info("Found {} popular courses", courses.getTotalElements());

        return courses.map(this::mapToListDTO);
    }

    /**
     * Get free courses.
     *
     * @param pageable Pagination parameters
     * @return Page of free course DTOs
     */
    public Page<CourseDTO> getFreeCourses(Pageable pageable) {
        log.debug("Fetching free courses");

        Page<Course> courses = courseRepository.findByPriceAndIsPublishedTrue(BigDecimal.ZERO, pageable);

        log.info("Found {} free courses", courses.getTotalElements());

        return courses.map(this::mapToListDTO);
    }

    // ==========================================
    // COURSE CREATION & MANAGEMENT (Instructors)
    // ==========================================

    /**
     * Create a new course (Instructor only).
     * Course starts in DRAFT status.
     *
     * @param request Course creation request
     * @param instructorId Instructor UUID (from JWT token)
     * @return Created course detail DTO
     * @throws RuntimeException if category not found or validation fails
     */
    @Transactional
    public CourseDetailDTO createCourse(CreateCourseRequest request, UUID instructorId) {
        log.debug("Creating course: {} by instructor: {}", request.getTitle(), instructorId);

        // Validate category exists
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", request.getCategoryId());
                    return new RuntimeException("Category not found with ID: " + request.getCategoryId());
                });

        // Convert lists to JSON strings
        String learningObjectivesJson = serializeList(request.getLearningObjectives());
        String requirementsJson = request.getRequirements() != null ?
                serializeList(request.getRequirements()) : null;

        // Create course entity
        Course course = Course.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .shortDescription(request.getShortDescription() != null ?
                        request.getShortDescription().trim() : null)
                .learningObjectives(learningObjectivesJson)
                .requirements(requirementsJson)
                .category(category)
                .instructorId(instructorId)
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .level(CourseLevel.valueOf(request.getLevel()))  // Convert string to enum
                .language(request.getLanguage())
                .status(CourseStatus.DRAFT)  // Use enum
                .isPublished(false)
                .isFeatured(false)
                .enrollmentCount(0)
                .averageRating(BigDecimal.ZERO)
                .ratingCount(0)
                .durationMinutes(0)
                .lectureCount(0)
                .build();

        // Save course
        Course savedCourse = courseRepository.save(course);

        log.info("Created course with ID: {} and slug: {}",
                savedCourse.getId(), savedCourse.getSlug());

        return mapToDetailDTO(savedCourse);
    }

    /**
     * Get courses by instructor.
     *
     * @param instructorId Instructor UUID
     * @param pageable Pagination parameters
     * @return Page of instructor's course DTOs
     */
    public Page<CourseDTO> getCoursesByInstructor(UUID instructorId, Pageable pageable) {
        log.debug("Fetching courses for instructor: {}", instructorId);

        Page<Course> courses = courseRepository.findByInstructorId(instructorId, pageable);

        log.info("Found {} courses for instructor", courses.getTotalElements());

        return courses.map(this::mapToListDTO);
    }

    // ==========================================
    // DTO MAPPING
    // ==========================================

    /**
     * Map Course entity to CourseDTO (for list views).
     *
     * @param course Course entity
     * @return Course DTO
     */
    private CourseDTO mapToListDTO(Course course) {
        return CourseDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .shortDescription(course.getShortDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .price(course.getPrice())
                .originalPrice(course.getOriginalPrice())
                .discountPercentage(course.getDiscountPercentage())
                .level(course.getLevel().name())  // Convert enum to string
                .language(course.getLanguage())
                .durationMinutes(course.getDurationMinutes())
                .lectureCount(course.getLectureCount())
                .enrollmentCount(course.getEnrollmentCount())
                .averageRating(course.getAverageRating())
                .ratingCount(course.getRatingCount())
                .isFeatured(course.getIsFeatured())
                .isFree(course.isFree())
                .category(mapCategoryToDTO(course.getCategory()))
                .instructorId(course.getInstructorId())
                .instructorName("Instructor Name") // TODO: Fetch from User Service when available
                .build();
    }

    /**
     * Map Course entity to CourseDetailDTO (for detail view).
     *
     * @param course Course entity
     * @return Course detail DTO
     */
    private CourseDetailDTO mapToDetailDTO(Course course) {
        return CourseDetailDTO.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .shortDescription(course.getShortDescription())
                .learningObjectives(deserializeList(course.getLearningObjectives()))
                .requirements(deserializeList(course.getRequirements()))
                .thumbnailUrl(course.getThumbnailUrl())
                .previewVideoUrl(course.getPreviewVideoUrl())
                .price(course.getPrice())
                .originalPrice(course.getOriginalPrice())
                .discountPercentage(course.getDiscountPercentage())
                .isFree(course.isFree())
                .level(course.getLevel().name())  // Convert enum to string
                .language(course.getLanguage())
                .status(course.getStatus().name())  // Convert enum to string
                .durationMinutes(course.getDurationMinutes())
                .lectureCount(course.getLectureCount())
                .enrollmentCount(course.getEnrollmentCount())
                .averageRating(course.getAverageRating())
                .ratingCount(course.getRatingCount())
                .isFeatured(course.getIsFeatured())
                .isPublished(course.getIsPublished())
                .category(mapCategoryToDTO(course.getCategory()))
                .instructorId(course.getInstructorId())
                .instructorName("Instructor Name") // TODO: Fetch from User Service
                .instructorBio("Instructor bio") // TODO: Fetch from User Service
                .publishedAt(course.getPublishedAt())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    /**
     * Map Category entity to CategoryDTO.
     *
     * @param category Category entity
     * @return Category DTO
     */
    private CategoryDTO mapCategoryToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .courseCount(category.getCourseCount())
                .build();
    }

    /**
     * Serialize list to JSON string.
     *
     * @param list List of strings
     * @return JSON string
     */
    private String serializeList(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize list", e);
            throw new RuntimeException("Failed to serialize list", e);
        }
    }

    /**
     * Deserialize JSON string to list.
     *
     * @param json JSON string
     * @return List of strings
     */
    private List<String> deserializeList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON", e);
            return new ArrayList<>();
        }
    }
}