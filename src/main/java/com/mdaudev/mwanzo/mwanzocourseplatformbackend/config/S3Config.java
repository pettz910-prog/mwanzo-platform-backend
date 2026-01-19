package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3 Configuration
 *
 * Configures AWS S3 client for LocalStack (development) or AWS S3 (production).
 * Uses shared AWS credentials provider from AwsConfig.
 *
 * DEV MODE: Points to LocalStack endpoint with path-style access
 * PROD MODE: Uses standard AWS S3 with virtual-hosted-style access
 *
 * @author Mwanzo Development Team
 * @version 2.0
 * @since 2026-01-18
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
@Data
@Slf4j
public class S3Config {

    private String endpoint;
    private String region;
    private String bucket;
    private boolean useLocalstack;

    /**
     * Create AmazonS3 client bean.
     * Uses shared credentials provider from AwsConfig.
     * Switches between LocalStack (dev) and AWS S3 (prod) based on configuration.
     *
     * @param credentialsProvider Injected from AwsConfig
     * @return Configured AmazonS3 client
     */
    @Bean
    public AmazonS3 amazonS3(AWSCredentialsProvider credentialsProvider) {
        log.info("🗄️ Initializing S3 client");
        log.info("🗄️ Mode: {}", useLocalstack ? "LocalStack (Development)" : "AWS S3 (Production)");
        log.info("🗄️ Region: {}", region);
        log.info("🗄️ Bucket: {}", bucket);

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(credentialsProvider);

        if (useLocalstack) {
            // LocalStack configuration (development)
            if (endpoint == null || endpoint.isEmpty()) {
                log.warn("⚠️ LocalStack enabled but endpoint not configured!");
                log.warn("⚠️ Set aws.s3.endpoint in application.yml (e.g., http://localhost:4566)");
                throw new IllegalStateException("LocalStack endpoint not configured");
            }

            log.info("🗄️ LocalStack Endpoint: {}", endpoint);
            log.info("🗄️ Path-style access: enabled");

            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpoint, region)
            ).withPathStyleAccessEnabled(true);  // Required for LocalStack

        } else {
            // AWS S3 configuration (production)
            log.info("🗄️ Using AWS S3 standard endpoint");
            builder.withRegion(region);
        }

        try {
            AmazonS3 s3Client = builder.build();
            log.info("✅ S3 client initialized successfully");

            // Validate bucket configuration
            validateBucketConfiguration();

            return s3Client;

        } catch (Exception e) {
            log.error("❌ Failed to initialize S3 client", e);
            throw new RuntimeException("S3 client initialization failed", e);
        }
    }

    /**
     * Validate S3 bucket configuration.
     * Logs warnings for common configuration issues.
     */
    private void validateBucketConfiguration() {
        if (bucket == null || bucket.isEmpty()) {
            log.error("❌ S3 bucket name not configured!");
            log.error("❌ Set aws.s3.bucket in application.yml");
            throw new IllegalStateException("S3 bucket not configured");
        }

        // Validate bucket name format
        if (!bucket.matches("^[a-z0-9][a-z0-9.-]*[a-z0-9]$")) {
            log.warn("⚠️ S3 bucket name '{}' may not follow AWS naming conventions", bucket);
            log.warn("⚠️ Bucket names must be 3-63 chars, lowercase, start/end with letter/number");
        }

        // Check bucket name length
        if (bucket.length() < 3 || bucket.length() > 63) {
            log.error("❌ Invalid S3 bucket name: '{}' (must be 3-63 characters)", bucket);
            throw new IllegalStateException("Invalid S3 bucket name length");
        }

        log.debug("✅ S3 bucket name validation passed: {}", bucket);
    }

    /**
     * Get S3 bucket name.
     * Used by services that need to reference the bucket.
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Get S3 endpoint.
     * Used by services that need to generate URLs.
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Get AWS region.
     * Used by services that need region information.
     */
    public String getRegion() {
        return region;
    }

    /**
     * Check if using LocalStack.
     * Used by services to adjust URL generation logic.
     */
    public boolean isUseLocalstack() {
        return useLocalstack;
    }
}