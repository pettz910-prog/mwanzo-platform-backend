package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;



/**
 * CloudFront Service
 *
 * Generates CDN URLs for fast video delivery.
 *
 * DEV MODE: Returns LocalStack S3 URLs
 * PROD MODE: Returns CloudFront CDN URLs with optional signed URL support
 *
 * @author Mwanzo Development Team
 * @version 2.0
 * @since 2026-01-18
 */
@Service
@Slf4j
public class CloudFrontService {

    @Value("${aws.cloudfront.enabled:false}")
    private boolean enabled;

    @Value("${aws.cloudfront.domain:}")
    private String domain;

    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region:us-east-1}")
    private String region;

    @Value("${aws.s3.use-localstack:false}")
    private boolean useLocalStack;

    /**
     * Log CloudFront configuration on startup.
     */
    @PostConstruct
    public void logConfiguration() {
        log.info("🌐 CloudFront Service Initialized");
        log.info("🌐 CloudFront Enabled: {}", enabled);

        if (enabled && !useLocalStack) {
            log.info("🌐 CloudFront Domain: {}", domain != null && !domain.isEmpty() ? domain : "NOT CONFIGURED");
            if (domain == null || domain.isEmpty()) {
                log.warn("⚠️ CloudFront enabled but domain not configured!");
                log.warn("⚠️ Set aws.cloudfront.domain in application.yml");
            }
        } else {
            log.info("🌐 Using direct S3/LocalStack URLs (CloudFront disabled)");
        }
    }

    /**
     * Generate video streaming URL.
     * Uses CloudFront in production, S3/LocalStack in development.
     *
     * @param s3Key S3 object key
     * @return Streaming URL (CloudFront or S3)
     * @throws IllegalArgumentException if s3Key is null or empty
     */
    public String generateStreamingUrl(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            log.error("❌ Cannot generate streaming URL with null/empty S3 key");
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        try {
            if (enabled && !useLocalStack) {
                // Production: CloudFront CDN
                if (domain == null || domain.isEmpty()) {
                    log.error("❌ CloudFront enabled but domain not configured");
                    throw new IllegalStateException("CloudFront domain not configured");
                }

                String url = domain.endsWith("/")
                        ? domain + s3Key
                        : domain + "/" + s3Key;

                log.debug("🌐 Generated CloudFront URL: {}", url);
                return url;

            } else {
                // Development: Direct S3 or LocalStack
                String url;

                if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
                    // LocalStack
                    url = String.format("%s/%s/%s", s3Endpoint, bucket, s3Key);
                    log.debug("🔧 Generated LocalStack URL: {}", url);
                } else {
                    // Standard AWS S3
                    url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, s3Key);
                    log.debug("🔧 Generated S3 URL: {}", url);
                }

                return url;
            }

        } catch (Exception e) {
            log.error("❌ Failed to generate streaming URL for key: {}", s3Key, e);
            throw new RuntimeException("Failed to generate streaming URL", e);
        }
    }

    /**
     * Generate streaming URL for specific quality.
     * Used when serving transcoded videos at different resolutions.
     *
     * @param baseS3Key Original S3 key (e.g., "videos/uuid/video.mp4")
     * @param quality Quality (360p, 720p, 1080p)
     * @return Streaming URL for that quality
     * @throws IllegalArgumentException if parameters are invalid
     */
    public String generateQualityUrl(String baseS3Key, String quality) {
        if (baseS3Key == null || baseS3Key.isEmpty()) {
            throw new IllegalArgumentException("Base S3 key cannot be null or empty");
        }
        if (quality == null || quality.isEmpty()) {
            throw new IllegalArgumentException("Quality cannot be null or empty");
        }

        try {
            // Convert: videos/uuid/video.mp4 -> processed/360p/uuid_360p.mp4
            String qualityKey = convertToQualityKey(baseS3Key, quality);
            log.debug("Generated quality key: {} for quality: {}", qualityKey, quality);
            return generateStreamingUrl(qualityKey);

        } catch (Exception e) {
            log.error("❌ Failed to generate quality URL for: {} (quality: {})", baseS3Key, quality, e);
            throw new RuntimeException("Failed to generate quality URL", e);
        }
    }

    /**
     * Convert original S3 key to quality-specific key.
     * Follows MediaConvert output naming convention.
     *
     * @param originalKey Original S3 key (e.g., "videos/abc-123/video.mp4")
     * @param quality Quality level (e.g., "360p", "720p", "1080p")
     * @return Quality-specific S3 key (e.g., "processed/360p/abc-123_360p.mp4")
     */
    private String convertToQualityKey(String originalKey, String quality) {
        try {
            // videos/abc-123/video.mp4 -> processed/360p/abc-123_360p.mp4
            String[] parts = originalKey.split("/");

            if (parts.length < 2) {
                log.warn("⚠️ Unexpected S3 key format: {}", originalKey);
                // Fallback: use original key with quality suffix
                return String.format("processed/%s/%s", quality, originalKey);
            }

            String uuid = parts[1];  // Extract UUID from path
            String processedFileName = uuid + "_" + quality + ".mp4";

            return String.format("processed/%s/%s", quality, processedFileName);

        } catch (Exception e) {
            log.error("❌ Error converting S3 key to quality key: {}", originalKey, e);
            throw new RuntimeException("Failed to convert S3 key", e);
        }
    }

    /**
     * Check if CloudFront is enabled.
     */
    public boolean isEnabled() {
        return enabled && !useLocalStack;
    }

    /**
     * Get base CDN domain.
     */
    public String getDomain() {
        return enabled ? domain : s3Endpoint + "/" + bucket;
    }
}