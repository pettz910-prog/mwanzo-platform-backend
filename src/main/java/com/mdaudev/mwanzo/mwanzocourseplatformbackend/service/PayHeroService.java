package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.config.PayHeroConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * PayHero Service
 *
 * Handles M-Pesa STK Push integration via PayHero API.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayHeroService {

    private final PayHeroConfig config;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    /**
     * Initiate PayHero STK Push.
     */
    public PayHeroResponse initiatePayment(String phone, double amount, String transactionRef)
            throws JsonProcessingException {

        log.info("💳 Initiating PayHero payment | phone={}, amount=KSh {}, ref={}",
                phone, amount, transactionRef);

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        String normalizedPhone = normalizePhone(phone);

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", (int) amount);
        payload.put("phone_number", normalizedPhone);
        payload.put("channel_id", Integer.parseInt(config.getChannelId()));
        payload.put("provider", "m-pesa");
        payload.put("external_reference", transactionRef);
        payload.put("callback_url", config.getCallbackUrl());

        log.debug("PayHero Payload: {}", objectMapper.writeValueAsString(payload));

        if (config.getApiUsername() == null || config.getApiPassword() == null) {
            throw new IllegalStateException("PayHero API credentials not configured");
        }

        String credentials = config.getApiUsername() + ":" + config.getApiPassword();
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", basicAuth);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<PayHeroResponse> response = restTemplate.postForEntity(
                    config.getBaseUrl(),
                    request,
                    PayHeroResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("PayHero returned non-success response");
            }

            PayHeroResponse payHeroResponse = response.getBody();
            log.info("✅ PayHero STK Push initiated: checkoutRequestId={}",
                    payHeroResponse.getCheckoutRequestId());

            return payHeroResponse;

        } catch (HttpClientErrorException e) {
            log.error("❌ PayHero API error: Status={}, Body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("Invalid PayHero credentials");
            }

            throw new RuntimeException("Failed to initiate STK push: " + e.getMessage());
        }
    }

    /**
     * Normalize phone number to 254XXXXXXXXX format.
     */
    public String normalizePhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            throw new IllegalArgumentException("Phone number cannot be null or empty");
        }

        String digitsOnly = phone.replaceAll("\\D", "");

        if (digitsOnly.startsWith("0")) {
            digitsOnly = "254" + digitsOnly.substring(1);
        } else if (digitsOnly.startsWith("+254")) {
            digitsOnly = digitsOnly.substring(1);
        } else if (!digitsOnly.startsWith("254")) {
            throw new IllegalArgumentException("Invalid Kenyan phone number: " + phone);
        }

        if (digitsOnly.length() != 12) {
            throw new IllegalArgumentException("Invalid phone number length: " + phone);
        }

        return digitsOnly;
    }

    @Data
    public static class PayHeroResponse {
        @JsonProperty("checkout_request_id")
        private String checkoutRequestId;

        @JsonProperty("external_reference")
        private String externalReference;

        @JsonProperty("message")
        private String message;
    }

    @Data
    public static class PayHeroCallback {
        @JsonProperty("status")
        private Boolean status;

        @JsonProperty("response")
        private PayHeroCallbackResponse response;

        @Data
        public static class PayHeroCallbackResponse {
            @JsonProperty("ExternalReference")
            private String externalReference;

            @JsonProperty("CheckoutRequestID")
            private String checkoutRequestId;

            @JsonProperty("ResultCode")
            private Integer resultCode;

            @JsonProperty("ResultDesc")
            private String resultDesc;

            @JsonProperty("Amount")
            private Double amount;

            @JsonProperty("MpesaReceiptNumber")
            private String mpesaReceiptNumber;

            @JsonProperty("Phone")
            private String phone;

            @JsonProperty("Status")
            private String status;
        }
    }
}