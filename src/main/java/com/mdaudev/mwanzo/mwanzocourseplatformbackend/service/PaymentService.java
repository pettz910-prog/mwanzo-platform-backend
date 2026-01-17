package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Course;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.Enrollment;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.Payment;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.PaymentStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto.PaymentRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto.PaymentResponse;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto.PaymentStatusResponse;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.CourseRepository;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.EnrollmentRepository;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment Service
 *
 * SECURITY: Amount always pulled from database (enrollment.pricePaid)
 * NEVER uses amount from payment request!
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final PayHeroService payHeroService;
    private final EnrollmentService enrollmentService;

    @Value("${payment.currency:KES}")
    private String paymentCurrency;

    @Value("${payment.timeout-minutes:5}")
    private int timeoutMinutes;

    /**
     * Initiate payment for enrollment.
     * SECURITY: Amount pulled from enrollment.pricePaid (database).
     */
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("💳 Initiating payment for enrollment: {}", request.getEnrollmentId());

        // Get enrollment
        Enrollment enrollment = enrollmentRepository.findById(request.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", request.getEnrollmentId()));

        // Check if enrollment already has successful payment
        if (paymentRepository.hasSuccessfulPayment(enrollment.getId())) {
            throw new IllegalStateException("Payment already completed for this enrollment");
        }

        // SECURITY: Get amount from database, NOT from request
        java.math.BigDecimal paymentAmount = enrollment.getPricePaid();

        log.info("💰 Payment amount from database: KSh {}", paymentAmount);

        // Generate transaction reference
        String transactionRef = generateTransactionReference();

        // Create payment record
        Payment payment = Payment.builder()
                .enrollmentId(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .courseId(enrollment.getCourseId())
                .transactionReference(transactionRef)
                .amount(paymentAmount)  // From database!
                .currency(paymentCurrency)
                .phoneNumber(request.getPhoneNumber())
                .paymentStatus(PaymentStatus.INITIATED)
                .paymentMethod("MPESA")
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        try {
            // Initiate PayHero STK Push
            PayHeroService.PayHeroResponse payHeroResponse = payHeroService.initiatePayment(
                    request.getPhoneNumber(),
                    paymentAmount.doubleValue(),
                    transactionRef
            );

            if (payHeroResponse != null && payHeroResponse.getCheckoutRequestId() != null) {
                savedPayment.setCheckoutRequestId(payHeroResponse.getCheckoutRequestId());
                savedPayment.setPaymentStatus(PaymentStatus.PENDING);
                paymentRepository.save(savedPayment);

                log.info("✅ STK Push initiated: transactionRef={}, amount=KSh {}",
                        transactionRef, paymentAmount);

            } else {
                savedPayment.markAsFailed("PayHero STK push failed", null, null);
                paymentRepository.save(savedPayment);

                log.error("❌ STK Push failed for enrollment: {}", enrollment.getId());
            }

        } catch (Exception e) {
            log.error("❌ Error initiating PayHero payment", e);
            savedPayment.markAsFailed("Payment initiation error: " + e.getMessage(), null, null);
            paymentRepository.save(savedPayment);

            throw new RuntimeException("Failed to initiate payment", e);
        }

        return mapToResponse(savedPayment);
    }

    /**
     * Get payment status (for frontend polling).
     */
    public PaymentStatusResponse getPaymentStatus(String transactionRef) {
        log.debug("🔍 Fetching payment status for: {}", transactionRef);

        Payment payment = paymentRepository.findByTransactionReference(transactionRef)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "transactionReference", transactionRef));

        // Check if payment is stale
        if (payment.isStale(timeoutMinutes)) {
            log.warn("⏱️ Payment {} is stale (>{}min old), marking as timeout", transactionRef, timeoutMinutes);
            payment.markAsFailed("Payment verification timeout - no response from M-Pesa", null, null);
            paymentRepository.save(payment);
        }

        return PaymentStatusResponse.builder()
                .paymentStatus(payment.getPaymentStatus().name())
                .transactionReference(payment.getTransactionReference())
                .mpesaReceiptNumber(payment.getMpesaReceiptNumber())
                .failureReason(payment.getFailureReason())
                .shouldContinuePolling(shouldContinuePolling(payment))
                .build();
    }

    private boolean shouldContinuePolling(Payment payment) {
        return payment.getPaymentStatus() == PaymentStatus.INITIATED ||
                payment.getPaymentStatus() == PaymentStatus.PENDING;
    }

    /**
     * Process payment callback from PayHero.
     */
    @Transactional
    public void processCallback(PayHeroService.PayHeroCallback callback) {
        log.info("📥 Processing payment callback");

        if (callback == null || callback.getResponse() == null) {
            log.error("❌ Invalid callback: null or missing response");
            return;
        }

        PayHeroService.PayHeroCallback.PayHeroCallbackResponse response = callback.getResponse();
        log.info("Processing callback - CheckoutRequestId: {}, ExternalRef: {}",
                response.getCheckoutRequestId(), response.getExternalReference());

        // Find payment
        Payment payment = paymentRepository.findByCheckoutRequestId(response.getCheckoutRequestId())
                .or(() -> paymentRepository.findByTransactionReference(response.getExternalReference()))
                .orElse(null);

        if (payment == null) {
            log.error("❌ Payment not found for checkoutRequestId: {} or externalReference: {}",
                    response.getCheckoutRequestId(), response.getExternalReference());
            return;
        }

        log.info("✅ Found payment: ID={}, Status={}, Amount=KSh {}",
                payment.getId(), payment.getPaymentStatus(), payment.getAmount());

        // Check if callback already processed
        if (payment.getCallbackReceived()) {
            log.info("⚠️ Callback already processed for transaction: {}", payment.getTransactionReference());
            return;
        }

        payment.setCallbackReceived(true);
        payment.setCallbackData(callback.toString());

        boolean isSuccess = (response.getResultCode() != null && response.getResultCode() == 0);

        if (isSuccess) {
            handleSuccessfulPayment(payment, response);
        } else {
            handleFailedPayment(payment, response);
        }
    }

    private void handleSuccessfulPayment(Payment payment,
                                         PayHeroService.PayHeroCallback.PayHeroCallbackResponse response) {
        log.info("🎯 Handling SUCCESS for payment: {}", payment.getTransactionReference());

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.info("⚠️ Payment already marked as SUCCESS. Ignoring duplicate.");
            return;
        }

        try {
            payment.markAsSuccess(
                    response.getMpesaReceiptNumber(),
                    response.getResultCode(),
                    response.getResultDesc()
            );
            paymentRepository.save(payment);

            // Activate enrollment
            enrollmentService.activateEnrollment(payment.getEnrollmentId(), payment.getId());

            log.info("✅ Payment SUCCESS: Receipt={}, Amount=KSh {}",
                    response.getMpesaReceiptNumber(), response.getAmount());

            // TODO: Send success email

        } catch (Exception e) {
            log.error("❌ Error handling successful payment", e);
            throw new RuntimeException("Failed to process successful payment", e);
        }
    }

    private void handleFailedPayment(Payment payment,
                                     PayHeroService.PayHeroCallback.PayHeroCallbackResponse response) {
        log.info("🎯 Handling FAILURE for payment: {}", payment.getTransactionReference());

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            log.warn("⚠️ Received FAILURE callback after success. Ignoring.");
            return;
        }

        try {
            payment.markAsFailed(
                    response.getResultDesc(),
                    response.getResultCode(),
                    response.getResultDesc()
            );
            paymentRepository.save(payment);

            log.info("❌ Payment FAILED: ResultCode={}, Reason={}",
                    response.getResultCode(), response.getResultDesc());

            // TODO: Send failure email

        } catch (Exception e) {
            log.error("❌ Error handling failed payment", e);
        }
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateTransactionReference() {
        return "MWZ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .enrollmentId(payment.getEnrollmentId())
                .transactionReference(payment.getTransactionReference())
                .checkoutRequestId(payment.getCheckoutRequestId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .phoneNumber(payment.getPhoneNumber())
                .paymentStatus(payment.getPaymentStatus().name())
                .mpesaReceiptNumber(payment.getMpesaReceiptNumber())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .completedAt(payment.getPaymentDate())
                .build();
    }
}