package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID enrollmentId;
    private String transactionReference;
    private String checkoutRequestId;
    private BigDecimal amount;
    private String currency;
    private String phoneNumber;
    private String paymentStatus;
    private String mpesaReceiptNumber;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}