package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.mediaconvert.AWSMediaConvert;
import com.amazonaws.services.mediaconvert.AWSMediaConvertClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * AWS Services Configuration
 *
 * Configures AWS clients for S3, MediaConvert, CloudFront, etc.
 * Supports both explicit credentials and IAM role-based authentication.
 *
 * DEVELOPMENT: Uses explicit credentials or LocalStack
 * PRODUCTION: Prefers IAM roles, falls back to explicit credentials
 *
 * @author Mwanzo Development Team
 * @version 2.0
 * @since 2026-01-18
 */
@Configuration
@Slf4j
public class AwsConfig {

    @Value("${aws.access-key:}")
    private String accessKey;

    @Value("${aws.secret-key:}")
    private String secretKey;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.use-iam-role:true}")
    private boolean useIamRole;

    // MediaConvert specific
    @Value("${aws.mediaconvert.enabled:false}")
    private boolean mediaConvertEnabled;

    @Value("${aws.mediaconvert.endpoint:}")
    private String mediaConvertEndpoint;

    /**
     * AWS Credentials Provider
     * Uses IAM role in production (recommended), explicit credentials as fallback
     */
    @Bean
    public AWSCredentialsProvider awsCredentialsProvider() {
        // Try IAM role first (EC2/ECS instance profile)
        if (useIamRole && (accessKey == null || accessKey.isEmpty())) {
            log.info("🔐 Using AWS IAM Role for credentials (recommended for production)");
            return DefaultAWSCredentialsProviderChain.getInstance();
        }

        // Fall back to explicit credentials
        if (accessKey != null && !accessKey.isEmpty() &&
            secretKey != null && !secretKey.isEmpty()) {
            log.info("🔐 Using explicit AWS credentials from configuration");
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            return new AWSStaticCredentialsProvider(credentials);
        }

        // Last resort: try default credential chain
        log.warn("⚠️ No explicit credentials configured, trying default AWS credential chain");
        return DefaultAWSCredentialsProviderChain.getInstance();
    }

    /**
     * AWS MediaConvert Client
     * Only created if MediaConvert is enabled and endpoint is configured
     */
    @Bean
    @Profile("!test") // Don't create in test profile
    public AWSMediaConvert awsMediaConvertClient(AWSCredentialsProvider credentialsProvider) {
        if (!mediaConvertEnabled) {
            log.info("📹 AWS MediaConvert is DISABLED (mock mode will be used)");
            return null;
        }

        if (mediaConvertEndpoint == null || mediaConvertEndpoint.isEmpty()) {
            log.warn("⚠️ MediaConvert enabled but no endpoint configured - using mock");
            return null;
        }

        log.info("📹 Initializing AWS MediaConvert client");
        log.info("📹 Endpoint: {}", mediaConvertEndpoint);
        log.info("📹 Region: {}", region);

        try {
            AWSMediaConvert client = AWSMediaConvertClientBuilder.standard()
                    .withCredentials(credentialsProvider)
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(
                                    mediaConvertEndpoint,
                                    region
                            )
                    )
                    .build();

            log.info("✅ AWS MediaConvert client initialized successfully");
            return client;

        } catch (Exception e) {
            log.error("❌ Failed to initialize MediaConvert client", e);
            log.warn("⚠️ Will fall back to mock mode for video transcoding");
            return null;
        }
    }

    /**
     * Validate AWS Configuration on startup
     */
    @Bean
    public AwsConfigValidator awsConfigValidator() {
        return new AwsConfigValidator(
                accessKey,
                secretKey,
                region,
                useIamRole,
                mediaConvertEnabled,
                mediaConvertEndpoint
        );
    }

    /**
     * AWS Configuration Validator
     * Logs configuration status on startup
     */
    public static class AwsConfigValidator {
        public AwsConfigValidator(
                String accessKey,
                String secretKey,
                String region,
                boolean useIamRole,
                boolean mediaConvertEnabled,
                String mediaConvertEndpoint
        ) {
            log.info("=".repeat(60));
            log.info("AWS CONFIGURATION");
            log.info("=".repeat(60));
            log.info("Region: {}", region);
            log.info("Use IAM Role: {}", useIamRole);
            log.info("Explicit Credentials: {}", hasCredentials(accessKey, secretKey) ? "✅ Configured" : "❌ Not configured");
            log.info("MediaConvert Enabled: {}", mediaConvertEnabled);

            if (mediaConvertEnabled) {
                log.info("MediaConvert Endpoint: {}",
                        (mediaConvertEndpoint != null && !mediaConvertEndpoint.isEmpty())
                                ? mediaConvertEndpoint
                                : "❌ Not configured (will use mock)");
            }

            log.info("=".repeat(60));

            // Warnings
            if (!useIamRole && !hasCredentials(accessKey, secretKey)) {
                log.warn("⚠️⚠️⚠️ WARNING: No AWS credentials configured! ⚠️⚠️⚠️");
                log.warn("Configure one of:");
                log.warn("  1. IAM Role (recommended for EC2/ECS)");
                log.warn("  2. Explicit credentials in application.yml");
                log.warn("  3. AWS credentials file (~/.aws/credentials)");
            }

            if (mediaConvertEnabled && (mediaConvertEndpoint == null || mediaConvertEndpoint.isEmpty())) {
                log.warn("⚠️ MediaConvert enabled but endpoint not configured");
                log.warn("Get endpoint from: AWS Console → MediaConvert → Account");
                log.warn("Will use mock transcoding for now");
            }
        }

        private static boolean hasCredentials(String accessKey, String secretKey) {
            return accessKey != null && !accessKey.isEmpty() &&
                   secretKey != null && !secretKey.isEmpty();
        }
    }
}
