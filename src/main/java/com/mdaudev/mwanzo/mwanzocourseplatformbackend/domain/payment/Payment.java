package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment Entity
 *
 * Tracks all payment transactions for course enrollments.
 * Supports M-Pesa (via PayHero) and future payment methods.
 *
 * Database Table: payments
 *
 * Payment Flow:
 * 1. INITIATED - Payment record created
 * 2. PENDING - STK push sent to user's phone
 * 3. SUCCESS - Payment confirmed by M-Pesa
 * 4. FAILED - Payment cancelled/rejected
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_transaction_ref", columnList = "transaction_reference"),
        @Index(name = "idx_payment_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_payment_student", columnList = "student_id"),
        @Index(name = "idx_payment_status", columnList = "payment_status"),
        @Index(name = "idx_payment_checkout", columnList = "checkout_request_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Enrollment this payment is for.
     */
    @Column(name = "enrollment_id", nullable = false)
    private UUID enrollmentId;

    /**
     * Student making the payment.
     */
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    /**
     * Course being purchased.
     */
    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    /**
     * Our internal transaction reference (e.g., MWZ-ABC12345).
     */
    @Column(name = "transaction_reference", nullable = false, unique = true, length = 50)
    private String transactionReference;

    /**
     * PayHero checkout request ID (from STK push).
     */
    @Column(name = "checkout_request_id", length = 100)
    private String checkoutRequestId;

    /**
     * M-Pesa receipt number (e.g., RBJ3K9X7M2).
     * Only set when payment is successful.
     */
    @Column(name = "mpesa_receipt_number", length = 50)
    private String mpesaReceiptNumber;

    /**
     * Amount paid.
     */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Currency (default: KES).
     */
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "KES";

    /**
     * Phone number used for payment (254XXXXXXXXX).
     */
    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    /**
     * Payment method (MPESA, CARD, etc.).
     */
    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "MPESA";

    /**
     * Payment status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.INITIATED;

    /**
     * Payer's name (from enrollment).
     */
    @Column(name = "payer_name", length = 100)
    private String payerName;

    /**
     * Payer's email (from user account).
     */
    @Column(name = "payer_email", length = 150)
    private String payerEmail;

    /**
     * M-Pesa result code from callback.
     */
    @Column(name = "result_code")
    private Integer resultCode;

    /**
     * M-Pesa result description.
     */
    @Column(name = "result_desc", length = 500)
    private String resultDesc;

    /**
     * Failure reason (for failed payments).
     */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Whether callback was received from PayHero.
     */
    @Column(name = "callback_received", nullable = false)
    @Builder.Default
    private Boolean callbackReceived = false;

    /**
     * Full callback data (for debugging).
     */
    @Column(name = "callback_data", columnDefinition = "TEXT")
    private String callbackData;

    /**
     * When payment was completed (SUCCESS or FAILED).
     */
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    /**
     * IP address of user when initiating payment.
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Mark payment as successful.
     */
    public void markAsSuccess(String mpesaReceipt, Integer resultCode, String resultDesc) {
        this.paymentStatus = PaymentStatus.SUCCESS;
        this.mpesaReceiptNumber = mpesaReceipt;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.paymentDate = LocalDateTime.now();
    }

    /**
     * Mark payment as failed.
     */
    public void markAsFailed(String reason, Integer resultCode, String resultDesc) {
        this.paymentStatus = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.paymentDate = LocalDateTime.now();
    }

    /**
     * Check if payment is in pending state.
     */
    public boolean isPending() {
        return paymentStatus == PaymentStatus.INITIATED ||
                paymentStatus == PaymentStatus.PENDING;
    }

    /**
     * Check if payment is stale (older than timeout).
     */
    public boolean isStale(int timeoutMinutes) {
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(timeoutMinutes);
        return isPending() && createdAt.isBefore(timeout);
    }
}