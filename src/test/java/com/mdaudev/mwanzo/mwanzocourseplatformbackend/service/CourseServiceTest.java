package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Category;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Course;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.CourseLevel;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.CourseStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CourseDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CourseDetailDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.CreateCourseRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CategoryRepository;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// ... existing code ...

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private CourseService courseService;

    private UUID courseId;
    private UUID categoryId;
    private UUID instructorId;
    private Category category;
    private Course course;

    @BeforeEach
    void setUp() {
        courseId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        instructorId = UUID.randomUUID();

        category = Category.builder()
                .id(categoryId)
                .name("Programming")
                .slug("programming")
                .courseCount(10)
                .build();

        course = Course.builder()
                .id(courseId)
                .title("Java Course")
                .slug("java-course")
                .description("Full Java course")
                .shortDescription("Short desc")
                .learningObjectives("[\"Objective 1\"]")
                .requirements("[\"Requirement 1\"]")
                .thumbnailUrl("http://example.com/thumb.png")
                .previewVideoUrl("http://example.com/preview.mp4")
                .price(new BigDecimal("10.00"))
                .originalPrice(new BigDecimal("20.00"))
                // .discountPercentage(new BigDecimal("50.00"))  // keep commented if not in entity
                .level(CourseLevel.BEGINNER)
                .language("en")
                .status(CourseStatus.PUBLISHED)
                .durationMinutes(120)
                .lectureCount(20)
                .enrollmentCount(100)
                .averageRating(new BigDecimal("4.5"))
                .ratingCount(50)
                .isFeatured(true)
                .isPublished(true)
                .category(category)
                .instructorId(instructorId)
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ... existing code ...

    @Test
    void getCourseById_whenCourseExists_returnsDetailDto() throws Exception {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(objectMapper.<List<String>>readValue(
                eq(course.getLearningObjectives()),
                any(com.fasterxml.jackson.core.type.TypeReference.class)
        )).thenReturn(List.of("Objective 1"));
        when(objectMapper.<List<String>>readValue(
                eq(course.getRequirements()),
                any(com.fasterxml.jackson.core.type.TypeReference.class)
        )).thenReturn(List.of("Requirement 1"));

        CourseDetailDTO dto = courseService.getCourseById(courseId);

        assertThat(dto.getId()).isEqualTo(courseId);
        assertThat(dto.getTitle()).isEqualTo("Java Course");
        assertThat(dto.getLearningObjectives()).containsExactly("Objective 1");
        assertThat(dto.getRequirements()).containsExactly("Requirement 1");
        assertThat(dto.getStatus()).isEqualTo(CourseStatus.PUBLISHED.name());
        assertThat(dto.getCategory().getId()).isEqualTo(categoryId);

        verify(courseRepository).findById(courseId);
    }

    // ... existing code ...

    @Test
    void createCourse_whenValidRequest_createsAndReturnsDetailDto() throws Exception {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTitle(" New Java Course ");
        request.setDescription("  Description  ");
        request.setShortDescription(" Short  ");
        request.setCategoryId(categoryId);
        request.setLearningObjectives(List.of("Objective 1", "Objective 2"));
        request.setRequirements(List.of("Req 1"));
        request.setPrice(new BigDecimal("15.00"));
        request.setOriginalPrice(new BigDecimal("30.00"));
        request.setLevel(CourseLevel.INTERMEDIATE.name());
        request.setLanguage("en");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(objectMapper.writeValueAsString(request.getLearningObjectives()))
                .thenReturn("[\"Objective 1\",\"Objective 2\"]");
        when(objectMapper.writeValueAsString(request.getRequirements()))
                .thenReturn("[\"Req 1\"]");

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);

        Course savedCourse = Course.builder()
                .id(courseId)
                .slug("new-java-course")
                .title("New Java Course")
                .description("Description")
                .shortDescription("Short")
                .learningObjectives("[\"Objective 1\",\"Objective 2\"]")
                .requirements("[\"Req 1\"]")
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .status(CourseStatus.DRAFT)
                .isPublished(false)
                .isFeatured(false)
                .level(CourseLevel.INTERMEDIATE)
                .language("en")
                .category(category)
                .instructorId(instructorId)
                .enrollmentCount(0)
                .lectureCount(0)
                .durationMinutes(0)
                .averageRating(BigDecimal.ZERO)
                .ratingCount(0)
                .build();

        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        when(objectMapper.<List<String>>readValue(
                eq(savedCourse.getLearningObjectives()),
                any(com.fasterxml.jackson.core.type.TypeReference.class)
        )).thenReturn(List.of("Objective 1", "Objective 2"));
        when(objectMapper.<List<String>>readValue(
                eq(savedCourse.getRequirements()),
                any(com.fasterxml.jackson.core.type.TypeReference.class)
        )).thenReturn(List.of("Req 1"));

        CourseDetailDTO dto = courseService.createCourse(request, instructorId);

        assertThat(dto.getId()).isEqualTo(courseId);
        assertThat(dto.getTitle()).isEqualTo("New Java Course");
        assertThat(dto.getStatus()).isEqualTo(CourseStatus.DRAFT.name());
        assertThat(dto.getIsPublished()).isFalse();
        assertThat(dto.getCategory().getId()).isEqualTo(categoryId);
        assertThat(dto.getInstructorId()).isEqualTo(instructorId);

        verify(categoryRepository).findById(categoryId);
        verify(objectMapper).writeValueAsString(request.getLearningObjectives());
        verify(objectMapper).writeValueAsString(request.getRequirements());
        verify(courseRepository).save(courseCaptor.capture());

        Course passedCourse = courseCaptor.getValue();
        assertThat(passedCourse.getTitle()).isEqualTo("New Java Course");
        assertThat(passedCourse.getDescription()).isEqualTo("Description");
        assertThat(passedCourse.getShortDescription()).isEqualTo("Short");
        assertThat(passedCourse.getStatus()).isEqualTo(CourseStatus.DRAFT);
        assertThat(passedCourse.getIsPublished()).isFalse();
        assertThat(passedCourse.getIsFeatured()).isFalse();
        assertThat(passedCourse.getEnrollmentCount()).isZero();
        assertThat(passedCourse.getAverageRating()).isEqualTo(BigDecimal.ZERO);
    }

    // ... existing code ...

    @Test
    void getCourseById_whenJsonInvalid_returnsEmptyLists() throws Exception {
        Course invalidCourse = Course.builder()
                .id(courseId)
                .title(course.getTitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .shortDescription(course.getShortDescription())
                .learningObjectives("invalid-json")
                .requirements("invalid-json")
                .thumbnailUrl(course.getThumbnailUrl())
                .previewVideoUrl(course.getPreviewVideoUrl())
                .price(course.getPrice())
                .originalPrice(course.getOriginalPrice())
                .level(course.getLevel())
                .language(course.getLanguage())
                .status(course.getStatus())
                .durationMinutes(course.getDurationMinutes())
                .lectureCount(course.getLectureCount())
                .enrollmentCount(course.getEnrollmentCount())
                .averageRating(course.getAverageRating())
                .ratingCount(course.getRatingCount())
                .isFeatured(course.getIsFeatured())
                .isPublished(course.getIsPublished())
                .category(category)
                .instructorId(instructorId)
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(invalidCourse));
        when(objectMapper.<List<String>>readValue(
                anyString(),
                any(com.fasterxml.jackson.core.type.TypeReference.class)
        )).thenThrow(new JsonProcessingException("bad json") {});

        CourseDetailDTO dto = courseService.getCourseById(courseId);

        assertThat(dto.getLearningObjectives()).isEmpty();
        assertThat(dto.getRequirements()).isEmpty();
    }

    // ... existing code ...
}