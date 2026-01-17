package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto.PaymentRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto.PaymentResponse;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.payment.dto.PaymentStatusResponse;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.PayHeroService;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment REST Controller
 *
 * Handles payment initiation, status polling, and PayHero callbacks.
 *
 * Base URL: /api/v1/payments
 *
 * Endpoints:
 * - POST   /api/v1/payments           - Initiate payment
 * - GET    /api/v1/payments/status/{ref} - Get payment status (polling)
 * - POST   /api/v1/payments/callback  - PayHero webhook (public)
 * - GET    /api/v1/payments           - Get all payments (admin)
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /**
     * Initiate payment for course enrollment.
     *
     * Flow:
     * 1. Creates payment record
     * 2. Sends M-Pesa STK push to user's phone
     * 3. Returns transaction reference for status polling
     *
     * Frontend should:
     * - Show "Check your phone" message
     * - Start polling /status/{transactionRef} every 3 seconds
     * - Stop polling after 60 seconds or when status is SUCCESS/FAILED
     *
     * Request Body:
     * {
     *   "enrollmentId": "uuid",
     *   "phoneNumber": "254712345678"
     * }
     *
     * @param request Payment request
     * @return Payment response with transaction reference
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        try {
            log.info("📝 Received payment request - Enrollment: {}, Phone: {}",
                    request.getEnrollmentId(), request.getPhoneNumber());

            PaymentResponse response = paymentService.initiatePayment(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.error("❌ State error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error initiating payment", e);
            throw new RuntimeException("Failed to initiate payment. Please try again.");
        }
    }

    /**
     * Get payment status (for frontend polling).
     *
     * Frontend should poll this endpoint every 3 seconds after initiating payment.
     * Stop polling when:
     * - paymentStatus is "SUCCESS" or "FAILED"
     * - shouldContinuePolling is false
     * - 60 seconds have elapsed
     *
     * Example response:
     * {
     *   "paymentStatus": "PENDING",
     *   "transactionReference": "MWZ-ABC12345",
     *   "mpesaReceiptNumber": null,
     *   "failureReason": null,
     *   "shouldContinuePolling": true
     * }
     *
     * @param transactionRef Transaction reference
     * @return Payment status response
     */
    @GetMapping("/status/{transactionRef}")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String transactionRef) {
        try {
            log.debug("🔍 GET /status/{} called", transactionRef);
            PaymentStatusResponse response = paymentService.getPaymentStatus(transactionRef);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Payment not found: " + transactionRef);
        }
    }

    /**
     * PayHero callback endpoint (webhook).
     *
     * IMPORTANT:
     * - This endpoint receives webhooks from PayHero when payment status changes
     * - Must be publicly accessible (no authentication)
     * - Always returns 200 OK to prevent PayHero from retrying
     * - In production, whitelist PayHero IPs for security
     *
     * PayHero sends POST request when:
     * - User completes payment on phone
     * - User cancels payment
     * - Payment times out
     *
     * Example PayHero callback:
     * {
     *   "status": true,
     *   "response": {
     *     "CheckoutRequestID": "ws_CO_12345",
     *     "ExternalReference": "MWZ-ABC12345",
     *     "ResultCode": 0,
     *     "ResultDesc": "Success",
     *     "Amount": 2999,
     *     "MpesaReceiptNumber": "RBJ3K9X7M2",
     *     "Phone": "254712345678",
     *     "Status": "Success"
     *   }
     * }
     *
     * @param callbackData Raw JSON callback from PayHero
     * @return 200 OK always
     */
    @PostMapping("/callback")
    public ResponseEntity<String> handlePayHeroCallback(@RequestBody String callbackData) {
        try {
            log.info("📥 Received PayHero callback");
            log.debug("Callback data: {}", callbackData);

            // Parse callback JSON
            PayHeroService.PayHeroCallback callback = objectMapper.readValue(
                    callbackData,
                    PayHeroService.PayHeroCallback.class
            );

            // Validate callback structure
            if (callback.getResponse() == null) {
                log.error("❌ Invalid callback: missing response object");
                return ResponseEntity.ok("Callback received but invalid structure");
            }

            // Process the callback (updates payment + activates enrollment)
            paymentService.processCallback(callback);

            log.info("✅ Callback processed successfully - Ref: {}",
                    callback.getResponse().getExternalReference());
            return ResponseEntity.ok("Callback processed successfully");

        } catch (Exception e) {
            log.error("❌ Error processing PayHero callback: {}", e.getMessage(), e);

            // IMPORTANT: Always return 200 to prevent PayHero from retrying
            return ResponseEntity.ok("Callback received");
        }
    }

    /**
     * Get all payments (Admin endpoint).
     * TODO: Add @PreAuthorize("hasRole('ADMIN')") when security is implemented
     *
     * @return List of all payments
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        log.info("GET /api/v1/payments - Fetching all payments");

        List<PaymentResponse> payments = paymentService.getAllPayments();

        return ResponseEntity.ok(payments);
    }
}