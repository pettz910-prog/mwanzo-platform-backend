package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment;

/**
 * Payment Status Enum
 *
 * Tracks the lifecycle of a payment transaction.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
public enum PaymentStatus {
    /**
     * Payment record created, STK push not yet sent.
     */
    INITIATED,

    /**
     * STK push sent to user's phone, waiting for confirmation.
     */
    PENDING,

    /**
     * Payment completed successfully, M-Pesa receipt received.
     */
    SUCCESS,

    /**
     * Payment failed, cancelled, or rejected by user.
     */
    FAILED,

    /**
     * Payment was successful but later refunded.
     */
    REFUNDED
}