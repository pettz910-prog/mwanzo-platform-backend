package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.MediaConvertService;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * MediaConvert Webhook Controller
 *
 * Receives AWS MediaConvert job completion notifications via SNS/HTTP webhook.
 *
 * AWS SNS sends notifications when transcoding jobs complete.
 * This endpoint processes those notifications and updates video records.
 *
 * Endpoint: POST /api/v1/webhooks/mediaconvert
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-17
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class MediaConvertWebhookController {

    private final VideoService videoService;
    private final ObjectMapper objectMapper;

    /**
     * Handle MediaConvert job completion webhook.
     *
     * AWS SNS sends POST request with job details when transcoding completes.
     *
     * Request body format:
     * {
     *   "Type": "Notification",
     *   "Message": "{\"jobId\":\"...\", \"status\":\"COMPLETE\", ...}"
     * }
     */
    @PostMapping("/mediaconvert")
    public ResponseEntity<Map<String, String>> handleMediaConvertWebhook(
            @RequestBody String requestBody) {

        log.info("📥 Received MediaConvert webhook");
        log.debug("Webhook payload: {}", requestBody);

        try {
            // Parse SNS notification
            JsonNode snsMessage = objectMapper.readTree(requestBody);

            // Check if it's an SNS subscription confirmation
            if (snsMessage.has("Type") && "SubscriptionConfirmation".equals(snsMessage.get("Type").asText())) {
                return handleSnsSubscriptionConfirmation(snsMessage);
            }

            // Extract the actual message
            String messageContent = snsMessage.has("Message")
                    ? snsMessage.get("Message").asText()
                    : requestBody;

            JsonNode message = objectMapper.readTree(messageContent);

            // Extract job details
            String jobId = extractJobId(message);
            String status = extractStatus(message);
            UUID videoId = extractVideoId(message);

            log.info("📋 Processing job: {} | Status: {} | Video: {}", jobId, status, videoId);

            // Build transcoding result
            MediaConvertService.TranscodingJobResult result = parseJobResult(message, videoId);

            // Update video record
            if (result.getStatus() == MediaConvertService.JobStatus.COMPLETE) {
                videoService.handleTranscodingComplete(videoId, result);
                log.info("✅ Video transcoding completed successfully: {}", videoId);
            } else {
                videoService.markVideoAsFailed(videoId, result.getErrorMessage());
                log.error("❌ Video transcoding failed: {}", videoId);
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Webhook processed",
                    "jobId", jobId
            ));

        } catch (Exception e) {
            log.error("❌ Failed to process MediaConvert webhook", e);

            // Return 200 anyway to prevent SNS retries
            return ResponseEntity.ok(Map.of(
                    "status", "error",
                    "message", "Failed to process webhook: " + e.getMessage()
            ));
        }
    }

    /**
     * Handle SNS subscription confirmation.
     *
     * When you first set up SNS → HTTP endpoint, AWS sends confirmation.
     * You need to visit the SubscribeURL to confirm.
     */
    private ResponseEntity<Map<String, String>> handleSnsSubscriptionConfirmation(JsonNode message) {
        String subscribeUrl = message.get("SubscribeURL").asText();

        log.info("📧 SNS Subscription Confirmation received");
        log.info("📧 Please visit this URL to confirm: {}", subscribeUrl);
        log.info("📧 Or we can auto-confirm by making GET request to that URL");

        // TODO: Auto-confirm by making HTTP GET to subscribeUrl
        // For now, manual confirmation required

        return ResponseEntity.ok(Map.of(
                "status", "confirmation_required",
                "message", "Visit SubscribeURL to confirm SNS subscription",
                "subscribeUrl", subscribeUrl
        ));
    }

    /**
     * Extract job ID from MediaConvert message.
     */
    private String extractJobId(JsonNode message) {
        // MediaConvert sends job details in different formats
        // Try multiple paths
        if (message.has("jobId")) {
            return message.get("jobId").asText();
        }
        if (message.has("detail") && message.get("detail").has("jobId")) {
            return message.get("detail").get("jobId").asText();
        }
        if (message.has("job") && message.get("job").has("id")) {
            return message.get("job").get("id").asText();
        }
        throw new IllegalArgumentException("Job ID not found in webhook message");
    }

    /**
     * Extract job status from MediaConvert message.
     */
    private String extractStatus(JsonNode message) {
        if (message.has("status")) {
            return message.get("status").asText();
        }
        if (message.has("detail") && message.get("detail").has("status")) {
            return message.get("detail").get("status").asText();
        }
        if (message.has("job") && message.get("job").has("status")) {
            return message.get("job").get("status").asText();
        }
        return "UNKNOWN";
    }

    /**
     * Extract video ID from MediaConvert job metadata.
     *
     * We embed video ID in job metadata when creating the job.
     */
    private UUID extractVideoId(JsonNode message) {
        // Look for video ID in userMetadata
        if (message.has("userMetadata") && message.get("userMetadata").has("videoId")) {
            return UUID.fromString(message.get("userMetadata").get("videoId").asText());
        }
        if (message.has("detail") && message.get("detail").has("userMetadata")) {
            JsonNode metadata = message.get("detail").get("userMetadata");
            if (metadata.has("videoId")) {
                return UUID.fromString(metadata.get("videoId").asText());
            }
        }
        throw new IllegalArgumentException("Video ID not found in webhook message");
    }

    /**
     * Parse MediaConvert job result into our domain object.
     */
    private MediaConvertService.TranscodingJobResult parseJobResult(JsonNode message, UUID videoId) {
        String jobId = extractJobId(message);
        String status = extractStatus(message);

        MediaConvertService.TranscodingJobResult.TranscodingJobResultBuilder builder =
                MediaConvertService.TranscodingJobResult.builder()
                        .videoId(videoId)
                        .jobId(jobId);

        // Map status
        if ("COMPLETE".equalsIgnoreCase(status)) {
            builder.status(MediaConvertService.JobStatus.COMPLETE);

            // Extract output URLs
            Map<String, String> qualityOutputs = extractOutputUrls(message);
            builder.qualityOutputs(qualityOutputs);

            // Extract duration
            Integer duration = extractDuration(message);
            builder.durationSeconds(duration);

        } else if ("ERROR".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
            builder.status(MediaConvertService.JobStatus.ERROR);

            // Extract error message
            String errorMessage = extractErrorMessage(message);
            builder.errorMessage(errorMessage);

        } else {
            builder.status(MediaConvertService.JobStatus.PROCESSING);
        }

        return builder.build();
    }

    /**
     * Extract output URLs from job result.
     */
    private Map<String, String> extractOutputUrls(JsonNode message) {
        Map<String, String> outputs = new HashMap<>();

        // MediaConvert outputs are in job.settings.outputGroups[]
        if (message.has("job") && message.get("job").has("settings")) {
            JsonNode outputGroups = message.get("job").get("settings").get("outputGroups");

            if (outputGroups != null && outputGroups.isArray()) {
                for (JsonNode group : outputGroups) {
                    String quality = group.has("name") ? group.get("name").asText() : "default";

                    // Get output destination
                    if (group.has("outputGroupSettings") &&
                            group.get("outputGroupSettings").has("fileGroupSettings")) {

                        String destination = group.get("outputGroupSettings")
                                .get("fileGroupSettings")
                                .get("destination")
                                .asText();

                        outputs.put(quality, destination);
                    }
                }
            }
        }

        // If no outputs found, return empty map (will use original video)
        if (outputs.isEmpty()) {
            log.warn("⚠️ No output URLs found in MediaConvert webhook");
        }

        return outputs;
    }

    /**
     * Extract video duration from job result.
     */
    private Integer extractDuration(JsonNode message) {
        // MediaConvert includes duration in output metadata
        if (message.has("job") && message.get("job").has("settings")) {
            JsonNode inputs = message.get("job").get("settings").get("inputs");

            if (inputs != null && inputs.isArray() && inputs.size() > 0) {
                JsonNode input = inputs.get(0);

                if (input.has("videoSelector") &&
                        input.get("videoSelector").has("programNumber")) {
                    // Duration might be in different places
                    // This is a placeholder - actual path depends on MediaConvert output
                }
            }
        }

        // Default: 0 (will need to be updated manually or via another method)
        log.warn("⚠️ Duration not found in MediaConvert webhook, defaulting to 0");
        return 0;
    }

    /**
     * Extract error message from failed job.
     */
    private String extractErrorMessage(JsonNode message) {
        if (message.has("errorMessage")) {
            return message.get("errorMessage").asText();
        }
        if (message.has("detail") && message.get("detail").has("errorMessage")) {
            return message.get("detail").get("errorMessage").asText();
        }
        return "Transcoding failed - no error message provided";
    }

    /**
     * Health check endpoint for webhook.
     */
    @GetMapping("/mediaconvert/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "MediaConvert Webhook",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}