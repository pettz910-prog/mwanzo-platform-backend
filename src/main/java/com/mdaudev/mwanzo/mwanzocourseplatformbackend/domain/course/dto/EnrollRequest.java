package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Enroll Request DTO
 *
 * Request body for enrolling in a course.
 * Can be used for single course enrollment or cart checkout.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollRequest {

    @NotNull(message = "Course ID is required")
    private UUID courseId;

    /**
     * Payment method: MPESA, CARD, FREE (for free courses)
     */
    private String paymentMethod;

    /**
     * M-Pesa phone number (if paymentMethod = MPESA)
     */
    private String phoneNumber;
}