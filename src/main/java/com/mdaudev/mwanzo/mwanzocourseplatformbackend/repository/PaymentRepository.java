package com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.Payment;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Payment Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTransactionReference(String transactionReference);

    Optional<Payment> findByCheckoutRequestId(String checkoutRequestId);

    List<Payment> findByEnrollmentId(UUID enrollmentId);

    List<Payment> findByStudentIdOrderByCreatedAtDesc(UUID studentId);

    @Query("SELECT p FROM Payment p WHERE p.enrollmentId = :enrollmentId AND p.paymentStatus = 'SUCCESS'")
    List<Payment> findSuccessfulPaymentsByEnrollmentId(@Param("enrollmentId") UUID enrollmentId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Payment p " +
            "WHERE p.enrollmentId = :enrollmentId AND p.paymentStatus = 'SUCCESS'")
    boolean hasSuccessfulPayment(@Param("enrollmentId") UUID enrollmentId);

    List<Payment> findByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status);

    List<Payment> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Payment> findRecentPayments(@Param("since") LocalDateTime since);

    @Query("SELECT p FROM Payment p WHERE p.paymentStatus IN ('INITIATED', 'PENDING') " +
            "AND p.createdAt < :timeout")
    List<Payment> findStalePendingPayments(@Param("timeout") LocalDateTime timeout);
}