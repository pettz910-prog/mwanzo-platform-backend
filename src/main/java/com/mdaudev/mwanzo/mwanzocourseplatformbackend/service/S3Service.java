package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.config.S3Config;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * S3 Service
 *
 * Handles video and thumbnail uploads to S3/LocalStack.
 * Generates presigned URLs for direct client uploads.
 * Provides streaming URLs for video playback.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-12
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 s3Client;
    private final S3Config s3Config;

    @Value("${cloudfront.domain}")
    private String cloudFrontDomain;

    @Value("${cloudfront.enabled}")
    private boolean cloudFrontEnabled;

    /**
     * Initialize S3 bucket (create if not exists).
     * Call this on application startup.
     */
    public void initializeBucket() {
        try {
            if (!s3Client.doesBucketExistV2(s3Config.getBucket())) {
                log.info("Creating S3 bucket: {}", s3Config.getBucket());
                s3Client.createBucket(s3Config.getBucket());
                log.info("✅ Bucket created successfully");
            } else {
                log.info("✅ S3 bucket already exists: {}", s3Config.getBucket());
            }
        } catch (Exception e) {
            log.error("❌ Failed to initialize S3 bucket", e);
            throw new RuntimeException("S3 initialization failed", e);
        }
    }

    /**
     * Generate presigned URL for video upload.
     * Frontend uses this to upload video directly to S3.
     *
     * @param fileName Original filename
     * @param contentType MIME type (e.g., "video/mp4")
     * @param expiry URL validity duration
     * @return Presigned upload URL and S3 key
     */
    public PresignedUploadUrl generateVideoUploadUrl(String fileName, String contentType, Duration expiry) {
        String s3Key = generateS3Key("videos", fileName);
        String presignedUrl = generatePresignedUploadUrl(s3Key, contentType, expiry);

        log.info("Generated video upload URL - S3 key: {}", s3Key);

        return PresignedUploadUrl.builder()
                .presignedUrl(presignedUrl)
                .s3Key(s3Key)
                .bucket(s3Config.getBucket())
                .expiresAt(Instant.now().plus(expiry))
                .build();
    }

    /**
     * Generate presigned URL for thumbnail upload.
     *
     * @param fileName Original filename
     * @param contentType MIME type (e.g., "image/jpeg")
     * @param expiry URL validity duration
     * @return Presigned upload URL and S3 key
     */
    public PresignedUploadUrl generateThumbnailUploadUrl(String fileName, String contentType, Duration expiry) {
        String s3Key = generateS3Key("thumbnails", fileName);
        String presignedUrl = generatePresignedUploadUrl(s3Key, contentType, expiry);

        log.info("Generated thumbnail upload URL - S3 key: {}", s3Key);

        return PresignedUploadUrl.builder()
                .presignedUrl(presignedUrl)
                .s3Key(s3Key)
                .bucket(s3Config.getBucket())
                .expiresAt(Instant.now().plus(expiry))
                .build();
    }

    /**
     * Generate presigned URL for PUT upload.
     */
    private String generatePresignedUploadUrl(String s3Key, String contentType, Duration expiry) {
        Date expiration = Date.from(Instant.now().plus(expiry));

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                s3Config.getBucket(),
                s3Key
        )
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);

        URL url = s3Client.generatePresignedUrl(request);
        return url.toString();
    }

    /**
     * Generate S3 key with UUID to prevent collisions.
     * Format: {prefix}/{uuid}/{filename}
     */
    private String generateS3Key(String prefix, String fileName) {
        String uuid = UUID.randomUUID().toString();
        String sanitizedFileName = sanitizeFileName(fileName);
        return String.format("%s/%s/%s", prefix, uuid, sanitizedFileName);
    }

    /**
     * Sanitize filename (remove special characters).
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Generate streaming URL for video playback.
     * Uses CloudFront in production, LocalStack in development.
     *
     * @param s3Key S3 object key
     * @return Public streaming URL
     */
    public String generateStreamingUrl(String s3Key) {
        if (cloudFrontEnabled) {
            // Production: CloudFront URL
            return String.format("%s/%s", cloudFrontDomain, s3Key);
        } else {
            // Development: LocalStack URL
            return String.format("%s/%s/%s",
                    s3Config.getEndpoint(),
                    s3Config.getBucket(),
                    s3Key);
        }
    }

    /**
     * Check if S3 object exists.
     *
     * @param s3Key S3 object key
     * @return true if object exists
     */
    public boolean objectExists(String s3Key) {
        try {
            s3Client.getObjectMetadata(s3Config.getBucket(), s3Key);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Get video metadata (file size, content type).
     *
     * @param s3Key S3 object key
     * @return Video metadata
     */
    public VideoMetadata getVideoMetadata(String s3Key) {
        ObjectMetadata metadata = s3Client.getObjectMetadata(s3Config.getBucket(), s3Key);

        return VideoMetadata.builder()
                .fileSizeBytes(metadata.getContentLength())
                .contentType(metadata.getContentType())
                .build();
    }

    /**
     * Delete object from S3.
     *
     * @param s3Key S3 object key
     */
    public void deleteObject(String s3Key) {
        try {
            s3Client.deleteObject(s3Config.getBucket(), s3Key);
            log.info("Deleted S3 object: {}", s3Key);
        } catch (Exception e) {
            log.error("Failed to delete S3 object: {}", s3Key, e);
            throw new RuntimeException("Failed to delete S3 object", e);
        }
    }
}

/**
 * Presigned Upload URL Response
 */
@Data
@Builder
class PresignedUploadUrl {
    private String presignedUrl;
    private String s3Key;
    private String bucket;
    private Instant expiresAt;
}

/**
 * Video Metadata
 */
@Data
@Builder
class VideoMetadata {
    private Long fileSizeBytes;
    private String contentType;
    private Integer durationSeconds;  // TODO: Extract from video file
}