package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment Status Response DTO
 *
 * Used for frontend polling.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusResponse {

    private String paymentStatus;
    private String transactionReference;
    private String mpesaReceiptNumber;
    private String failureReason;
    private Boolean shouldContinuePolling;
}