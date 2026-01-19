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

    @Value("${aws.cloudfront.domain:}")
    private String cloudFrontDomain;

    @Value("${aws.cloudfront.enabled:false}")
    private boolean cloudFrontEnabled;

    /**
     * Initialize S3 bucket (create if not exists).
     * Call this on application startup.
     * In production, bucket should already exist (managed by Terraform/CloudFormation).
     */
    public void initializeBucket() {
        String bucketName = s3Config.getBucket();

        try {
            log.info("🗄️ Checking S3 bucket: {}", bucketName);

            if (s3Client.doesBucketExistV2(bucketName)) {
                log.info("✅ S3 bucket exists: {}", bucketName);

                // Verify bucket access
                try {
                    s3Client.listObjectsV2(bucketName).getObjectSummaries();
                    log.info("✅ S3 bucket access verified");
                } catch (Exception e) {
                    log.error("❌ Cannot access S3 bucket - check IAM permissions", e);
                    throw new RuntimeException("S3 bucket access denied", e);
                }

            } else {
                log.warn("⚠️ S3 bucket does not exist: {}", bucketName);

                // Only auto-create in development (LocalStack)
                if (s3Config.isUseLocalstack()) {
                    log.info("🔧 Creating LocalStack bucket: {}", bucketName);
                    s3Client.createBucket(bucketName);
                    log.info("✅ LocalStack bucket created successfully");
                } else {
                    log.error("❌ Production bucket not found: {}", bucketName);
                    log.error("❌ Create bucket manually or use Terraform/CloudFormation");
                    throw new RuntimeException("Production S3 bucket does not exist: " + bucketName);
                }
            }

        } catch (AmazonS3Exception e) {
            log.error("❌ AWS S3 error during initialization", e);
            log.error("❌ Status Code: {}", e.getStatusCode());
            log.error("❌ Error Code: {}", e.getErrorCode());
            log.error("❌ Message: {}", e.getErrorMessage());
            throw new RuntimeException("S3 initialization failed", e);
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
        try {
            validateContentType(contentType, "video");

            String s3Key = generateS3Key("videos", fileName);
            String presignedUrl = generatePresignedUploadUrl(s3Key, contentType, expiry);

            log.info("✅ Generated video upload URL - S3 key: {}", s3Key);
            log.debug("Presigned URL expires in: {} minutes", expiry.toMinutes());

            return PresignedUploadUrl.builder()
                    .presignedUrl(presignedUrl)
                    .s3Key(s3Key)
                    .bucket(s3Config.getBucket())
                    .expiresAt(Instant.now().plus(expiry))
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to generate video upload URL for: {}", fileName, e);
            throw new RuntimeException("Failed to generate video upload URL", e);
        }
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
        try {
            validateContentType(contentType, "image");

            String s3Key = generateS3Key("thumbnails", fileName);
            String presignedUrl = generatePresignedUploadUrl(s3Key, contentType, expiry);

            log.info("✅ Generated thumbnail upload URL - S3 key: {}", s3Key);
            log.debug("Presigned URL expires in: {} minutes", expiry.toMinutes());

            return PresignedUploadUrl.builder()
                    .presignedUrl(presignedUrl)
                    .s3Key(s3Key)
                    .bucket(s3Config.getBucket())
                    .expiresAt(Instant.now().plus(expiry))
                    .build();

        } catch (Exception e) {
            log.error("❌ Failed to generate thumbnail upload URL for: {}", fileName, e);
            throw new RuntimeException("Failed to generate thumbnail upload URL", e);
        }
    }

    /**
     * Generate presigned URL for PUT upload.
     * Allows frontend to upload directly to S3 without going through backend.
     *
     * @param s3Key S3 object key
     * @param contentType MIME type
     * @param expiry URL validity duration
     * @return Presigned URL string
     */
    private String generatePresignedUploadUrl(String s3Key, String contentType, Duration expiry) {
        try {
            Date expiration = Date.from(Instant.now().plus(expiry));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    s3Config.getBucket(),
                    s3Key
            )
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(expiration)
                    .withContentType(contentType);

            URL url = s3Client.generatePresignedUrl(request);
            log.debug("Generated presigned URL for key: {}", s3Key);

            return url.toString();

        } catch (Exception e) {
            log.error("❌ Failed to generate presigned URL for key: {}", s3Key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
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
     * Prevents path traversal and invalid characters in S3 keys.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Remove path traversal attempts
        fileName = fileName.replace("..", "").replace("/", "_").replace("\\", "_");

        // Replace special characters with underscore
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Ensure filename is not too long (S3 key max is 1024 chars, leave room for prefix)
        if (sanitized.length() > 200) {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0) {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, 200 - extension.length()) + extension;
            } else {
                sanitized = sanitized.substring(0, 200);
            }
            log.warn("⚠️ Filename truncated to 200 characters: {}", sanitized);
        }

        return sanitized;
    }

    /**
     * Validate content type for uploads.
     * Prevents uploading files with incorrect MIME types.
     *
     * @param contentType MIME type to validate
     * @param expectedType Expected type prefix (e.g., "video", "image")
     */
    private void validateContentType(String contentType, String expectedType) {
        if (contentType == null || contentType.isEmpty()) {
            throw new IllegalArgumentException("Content type cannot be null or empty");
        }

        if (!contentType.toLowerCase().startsWith(expectedType.toLowerCase())) {
            log.error("❌ Invalid content type: {} (expected {}/*)", contentType, expectedType);
            throw new IllegalArgumentException(
                    String.format("Invalid content type: %s (expected %s/*)", contentType, expectedType)
            );
        }

        log.debug("✅ Content type validated: {}", contentType);
    }

    /**
     * Generate streaming URL for video playback.
     * Uses CloudFront in production, LocalStack in development.
     *
     * @param s3Key S3 object key
     * @return Public streaming URL
     */
    public String generateStreamingUrl(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        try {
            String streamingUrl;

            if (cloudFrontEnabled && cloudFrontDomain != null && !cloudFrontDomain.isEmpty()) {
                // Production: CloudFront CDN URL
                streamingUrl = cloudFrontDomain.endsWith("/")
                        ? cloudFrontDomain + s3Key
                        : cloudFrontDomain + "/" + s3Key;

                log.debug("🌐 Generated CloudFront URL: {}", streamingUrl);

            } else {
                // Development: Direct S3/LocalStack URL
                String endpoint = s3Config.getEndpoint();
                String bucket = s3Config.getBucket();

                if (endpoint == null || endpoint.isEmpty()) {
                    // Use standard AWS S3 URL format
                    streamingUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                            bucket, s3Config.getRegion(), s3Key);
                } else {
                    // Use custom endpoint (LocalStack)
                    streamingUrl = String.format("%s/%s/%s", endpoint, bucket, s3Key);
                }

                log.debug("🔧 Generated S3 URL: {}", streamingUrl);
            }

            return streamingUrl;

        } catch (Exception e) {
            log.error("❌ Failed to generate streaming URL for key: {}", s3Key, e);
            throw new RuntimeException("Failed to generate streaming URL", e);
        }
    }

    /**
     * Check if S3 object exists.
     *
     * @param s3Key S3 object key
     * @return true if object exists, false otherwise
     */
    public boolean objectExists(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            log.warn("⚠️ Attempted to check existence with null/empty S3 key");
            return false;
        }

        try {
            s3Client.getObjectMetadata(s3Config.getBucket(), s3Key);
            log.debug("✅ S3 object exists: {}", s3Key);
            return true;

        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                log.debug("S3 object not found: {}", s3Key);
                return false;
            }
            log.error("❌ Error checking S3 object existence: {}", s3Key, e);
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
        if (s3Key == null || s3Key.isEmpty()) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(s3Config.getBucket(), s3Key);

            VideoMetadata videoMetadata = VideoMetadata.builder()
                    .fileSizeBytes(metadata.getContentLength())
                    .contentType(metadata.getContentType())
                    .build();

            log.debug("✅ Retrieved metadata for: {} (size: {} bytes, type: {})",
                    s3Key, videoMetadata.getFileSizeBytes(), videoMetadata.getContentType());

            return videoMetadata;

        } catch (AmazonS3Exception e) {
            log.error("❌ Failed to get video metadata for: {}", s3Key, e);
            throw new RuntimeException("Failed to retrieve video metadata", e);
        }
    }

    /**
     * Delete object from S3.
     * Used when cleaning up failed uploads or removing content.
     *
     * @param s3Key S3 object key
     */
    public void deleteObject(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            log.warn("⚠️ Attempted to delete with null/empty S3 key");
            return;
        }

        try {
            // Check if object exists before deleting
            if (!objectExists(s3Key)) {
                log.warn("⚠️ Attempted to delete non-existent object: {}", s3Key);
                return;
            }

            s3Client.deleteObject(s3Config.getBucket(), s3Key);
            log.info("✅ Deleted S3 object: {}", s3Key);

        } catch (AmazonS3Exception e) {
            log.error("❌ AWS S3 error deleting object: {}", s3Key, e);
            log.error("❌ Status Code: {}, Error Code: {}", e.getStatusCode(), e.getErrorCode());
            throw new RuntimeException("Failed to delete S3 object: " + s3Key, e);

        } catch (Exception e) {
            log.error("❌ Unexpected error deleting S3 object: {}", s3Key, e);
            throw new RuntimeException("Failed to delete S3 object: " + s3Key, e);
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