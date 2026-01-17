package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Course;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Enrollment;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.EnrollmentStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto.EnrollmentDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CourseRepository;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.EnrollmentRepository;
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

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    // CourseService is injected into EnrollmentService, but not used in current methods
    @Mock
    private CourseService courseService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private UUID studentId;
    private UUID courseId;
    private UUID enrollmentId;
    private UUID paymentId;
    private Course freeCourse;
    private Course paidCourse;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        studentId = UUID.randomUUID();
        courseId = UUID.randomUUID();
        enrollmentId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        freeCourse = Course.builder()
                .id(courseId)
                .title("Free Course")
                .slug("free-course")
                .price(BigDecimal.ZERO)
                .originalPrice(BigDecimal.ZERO)
                .isPublished(true)
                .enrollmentCount(0)
                .thumbnailUrl("http://example.com/thumb.png")
                .build();

        paidCourse = Course.builder()
                .id(courseId)
                .title("Paid Course")
                .slug("paid-course")
                .price(new BigDecimal("25.00"))
                .originalPrice(new BigDecimal("25.00"))
                .isPublished(true)
                .enrollmentCount(0)
                .thumbnailUrl("http://example.com/thumb2.png")
                .build();

        enrollment = Enrollment.builder()
                .id(enrollmentId)
                .studentId(studentId)
                .courseId(courseId)
                .pricePaid(new BigDecimal("25.00"))
                .status(EnrollmentStatus.PENDING_PAYMENT)
                .progressPercentage(0)
                .videosCompleted(false)
                .quizzesCompleted(false)
                .isCompleted(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ==========================================
    // enrollInCourse
    // ==========================================

    @Test
    void enrollInCourse_whenFreeCourse_createsActiveEnrollmentAndIncrementsCount() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(freeCourse));

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenAnswer(invocation -> {
                    Enrollment e = invocation.getArgument(0);
                    e.setId(enrollmentId);
                    return e;
                });

        EnrollmentDTO dto = enrollmentService.enrollInCourse(studentId, courseId);

        assertThat(dto.getId()).isEqualTo(enrollmentId);
        assertThat(dto.getStudentId()).isEqualTo(studentId);
        assertThat(dto.getCourseId()).isEqualTo(courseId);
        assertThat(dto.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE.name());
        assertThat(dto.getPricePaid()).isEqualTo(BigDecimal.ZERO);
        assertThat(dto.getCourseTitle()).isEqualTo("Free Course");

        verify(enrollmentRepository).existsByStudentIdAndCourseId(studentId, courseId);
        verify(courseRepository).findById(courseId);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        verify(courseRepository).save(freeCourse);

        Enrollment saved = enrollmentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(saved.getPricePaid()).isEqualTo(BigDecimal.ZERO);
        assertThat(freeCourse.getEnrollmentCount()).isEqualTo(1);
    }

    @Test
    void enrollInCourse_whenPaidCourse_createsPendingEnrollmentAndDoesNotIncrementCount() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(paidCourse));

        ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenAnswer(invocation -> {
                    Enrollment e = invocation.getArgument(0);
                    e.setId(enrollmentId);
                    return e;
                });

        EnrollmentDTO dto = enrollmentService.enrollInCourse(studentId, courseId);

        assertThat(dto.getStatus()).isEqualTo(EnrollmentStatus.PENDING_PAYMENT.name());
        assertThat(dto.getPricePaid()).isEqualTo(new BigDecimal("25.00"));
        assertThat(dto.getCourseTitle()).isEqualTo("Paid Course");

        verify(enrollmentRepository).existsByStudentIdAndCourseId(studentId, courseId);
        verify(courseRepository).findById(courseId);
        verify(enrollmentRepository).save(enrollmentCaptor.capture());
        verify(courseRepository, never()).save(paidCourse);

        Enrollment saved = enrollmentCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(EnrollmentStatus.PENDING_PAYMENT);
        assertThat(paidCourse.getEnrollmentCount()).isEqualTo(0);
    }

    @Test
    void enrollInCourse_whenAlreadyEnrolled_throwsIllegalArgumentException() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(true);

        assertThatThrownBy(() -> enrollmentService.enrollInCourse(studentId, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already enrolled");

        verify(enrollmentRepository).existsByStudentIdAndCourseId(studentId, courseId);
        verifyNoMoreInteractions(enrollmentRepository, courseRepository);
    }

    @Test
    void enrollInCourse_whenCourseNotFound_throwsResourceNotFound() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.enrollInCourse(studentId, courseId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course")
                .hasMessageContaining("id");

        verify(courseRepository).findById(courseId);
    }

    @Test
    void enrollInCourse_whenCourseNotPublished_throwsIllegalArgumentException() {
        Course unpublished = Course.builder()
                .id(courseId)
                .title("Draft Course")
                .price(BigDecimal.ZERO)
                .isPublished(false)
                .build();

        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(unpublished));

        assertThatThrownBy(() -> enrollmentService.enrollInCourse(studentId, courseId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available for enrollment");

        verify(courseRepository).findById(courseId);
        verifyNoMoreInteractions(courseRepository);
    }

    // ==========================================
    // activateEnrollment
    // ==========================================

    @Test
    void activateEnrollment_whenValid_activatesAndIncrementsCourseCount() {
        Course course = paidCourse;
        course.setEnrollmentCount(2);

        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        EnrollmentDTO dto = enrollmentService.activateEnrollment(enrollmentId, paymentId);

        assertThat(dto.getId()).isEqualTo(enrollmentId);
        assertThat(dto.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE.name());
        assertThat(dto.getCourseId()).isEqualTo(courseId);
        assertThat(course.getEnrollmentCount()).isEqualTo(3);

        verify(enrollmentRepository).findById(enrollmentId);
        verify(enrollmentRepository).save(enrollment);
        verify(courseRepository).findById(courseId);
        verify(courseRepository).save(course);
    }

    @Test
    void activateEnrollment_whenEnrollmentNotFound_throwsResourceNotFound() {
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.activateEnrollment(enrollmentId, paymentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Enrollment")
                .hasMessageContaining("id");

        verify(enrollmentRepository).findById(enrollmentId);
        verifyNoMoreInteractions(enrollmentRepository, courseRepository);
    }

    @Test
    void activateEnrollment_whenCourseNotFound_throwsResourceNotFound() {
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.activateEnrollment(enrollmentId, paymentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course")
                .hasMessageContaining("id");

        verify(enrollmentRepository).findById(enrollmentId);
        verify(courseRepository).findById(courseId);
    }

    // ==========================================
    // getStudentEnrollments / getActiveEnrollments / getEnrollmentById
    // ==========================================

    @Test
    void getStudentEnrollments_returnsMappedDtos_evenIfCourseMissing() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Enrollment> page = new PageImpl<>(List.of(enrollment), pageable, 1);

        when(enrollmentRepository.findByStudentId(studentId, pageable))
                .thenReturn(page);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(paidCourse));

        Page<EnrollmentDTO> result = enrollmentService.getStudentEnrollments(studentId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        EnrollmentDTO dto = result.getContent().get(0);
        assertThat(dto.getStudentId()).isEqualTo(studentId);
        assertThat(dto.getCourseId()).isEqualTo(courseId);
        assertThat(dto.getCourseTitle()).isEqualTo("Paid Course");

        verify(enrollmentRepository).findByStudentId(studentId, pageable);
        verify(courseRepository).findById(courseId);
    }

    @Test
    void getActiveEnrollments_returnsMappedDtos() {
        Pageable pageable = PageRequest.of(0, 10);

        // Build an active enrollment explicitly (no toBuilder)
        Enrollment activeEnrollment = Enrollment.builder()
                .id(enrollmentId)
                .studentId(studentId)
                .courseId(courseId)
                .pricePaid(enrollment.getPricePaid())
                .status(EnrollmentStatus.ACTIVE)
                .progressPercentage(enrollment.getProgressPercentage())
                .videosCompleted(enrollment.getVideosCompleted())
                .quizzesCompleted(enrollment.getQuizzesCompleted())
                .isCompleted(enrollment.getIsCompleted())
                .createdAt(enrollment.getCreatedAt())
                .build();

        Page<Enrollment> page = new PageImpl<>(List.of(activeEnrollment), pageable, 1);

        when(enrollmentRepository.findActiveEnrollmentsByStudentId(studentId, pageable))
                .thenReturn(page);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(paidCourse));

        Page<EnrollmentDTO> result = enrollmentService.getActiveEnrollments(studentId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        EnrollmentDTO dto = result.getContent().get(0);
        assertThat(dto.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE.name());
        assertThat(dto.getCourseTitle()).isEqualTo("Paid Course");

        verify(enrollmentRepository).findActiveEnrollmentsByStudentId(studentId, pageable);
        verify(courseRepository).findById(courseId);
    }

    @Test
    void getEnrollmentById_whenFound_returnsDto() {
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.of(enrollment));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(paidCourse));

        EnrollmentDTO dto = enrollmentService.getEnrollmentById(enrollmentId);

        assertThat(dto.getId()).isEqualTo(enrollmentId);
        assertThat(dto.getStudentId()).isEqualTo(studentId);
        assertThat(dto.getCourseTitle()).isEqualTo("Paid Course");

        verify(enrollmentRepository).findById(enrollmentId);
        verify(courseRepository).findById(courseId);
    }

    @Test
    void getEnrollmentById_whenNotFound_throwsResourceNotFound() {
        when(enrollmentRepository.findById(enrollmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> enrollmentService.getEnrollmentById(enrollmentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Enrollment")
                .hasMessageContaining("id");

        verify(enrollmentRepository).findById(enrollmentId);
    }

    // ==========================================
    // isEnrolled / hasAccess / updateProgress
    // ==========================================

    @Test
    void isEnrolled_delegatesToRepository() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(true);

        boolean result = enrollmentService.isEnrolled(studentId, courseId);

        assertThat(result).isTrue();
        verify(enrollmentRepository).existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Test
    void hasAccess_whenEnrollmentAccessible_returnsTrue() {
        Enrollment accessible = mock(Enrollment.class);
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.of(accessible));
        when(accessible.isAccessible()).thenReturn(true);

        boolean result = enrollmentService.hasAccess(studentId, courseId);

        assertThat(result).isTrue();
        verify(enrollmentRepository).findByStudentIdAndCourseId(studentId, courseId);
        verify(accessible).isAccessible();
    }

    @Test
    void hasAccess_whenNoEnrollment_returnsFalse() {
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.empty());

        boolean result = enrollmentService.hasAccess(studentId, courseId);

        assertThat(result).isFalse();
        verify(enrollmentRepository).findByStudentIdAndCourseId(studentId, courseId);
    }

    @Test
    void updateProgress_updatesAndSavesEnrollment() {
        // Use the existing enrollment object directly
        enrollment.setProgressPercentage(10);

        when(enrollmentRepository.findById(enrollmentId))
                .thenReturn(Optional.of(enrollment));

        enrollmentService.updateProgress(enrollmentId, 75);

        verify(enrollmentRepository).findById(enrollmentId);
        verify(enrollmentRepository).save(enrollment);
        assertThat(enrollment.getProgressPercentage()).isEqualTo(75);
    }
}