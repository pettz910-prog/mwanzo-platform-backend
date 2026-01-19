package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.mediaconvert.AWSMediaConvert;
import com.amazonaws.services.mediaconvert.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AWS MediaConvert Service
 *
 * Handles video transcoding to multiple qualities (360p, 720p, 1080p).
 *
 * DEV MODE: Mocks transcoding with simulated delay
 * PROD MODE: Real AWS MediaConvert API calls
 *
 * @author Mwanzo Development Team
 * @version 2.0
 * @since 2026-01-18
 */
@Service
@Slf4j
public class MediaConvertService {

    private final S3Service s3Service;

    // MediaConvert client is now injected from AwsConfig
    // Can be null if MediaConvert is disabled or not configured
    private final AWSMediaConvert mediaConvertClient;

    @Value("${aws.mediaconvert.enabled:false}")
    private boolean enabled;

    @Value("${aws.mediaconvert.use-mock:true}")
    private boolean useMock;

    @Value("${aws.mediaconvert.mock-processing-seconds:30}")
    private int mockProcessingSeconds;

    @Value("${aws.mediaconvert.role-arn:}")
    private String roleArn;

    @Value("${aws.mediaconvert.queue-arn:}")
    private String queueArn;

    @Value("${aws.s3.bucket}")
    private String inputBucket;

    @Value("${aws.mediaconvert.output-bucket:}")
    private String outputBucket;

    @Value("${aws.s3.region}")
    private String region;

    /**
     * Constructor with dependency injection.
     * MediaConvert client can be null if not configured (will use mock mode).
     */
    @Autowired
    public MediaConvertService(
            S3Service s3Service,
            @Autowired(required = false) AWSMediaConvert mediaConvertClient
    ) {
        this.s3Service = s3Service;
        this.mediaConvertClient = mediaConvertClient;

        // Log initialization status
        if (mediaConvertClient != null) {
            log.info("📹 MediaConvert service initialized with REAL client");
        } else {
            log.info("📹 MediaConvert service initialized in MOCK mode (no client available)");
        }
    }

    /**
     * Create transcoding job for uploaded video.
     * Automatically selects mock or real mode based on configuration.
     *
     * @param videoId Video UUID
     * @param s3Key S3 key of original video
     * @return CompletableFuture with transcoding job result
     */
    @Async
    public CompletableFuture<TranscodingJobResult> createTranscodingJob(
            UUID videoId, String s3Key) {

        if (videoId == null || s3Key == null || s3Key.isEmpty()) {
            log.error("❌ Invalid parameters - videoId or s3Key is null/empty");
            return CompletableFuture.completedFuture(
                    TranscodingJobResult.failed(videoId, s3Key, "Invalid parameters"));
        }

        if (!enabled) {
            log.warn("⚠️ MediaConvert disabled - skipping transcoding");
            return CompletableFuture.completedFuture(
                    TranscodingJobResult.disabled(videoId, s3Key));
        }

        // Use mock if explicitly requested OR if no real client is available
        if (useMock || mediaConvertClient == null) {
            return mockTranscodingJob(videoId, s3Key);
        } else {
            return realTranscodingJob(videoId, s3Key);
        }
    }

    /**
     * Mock transcoding job (for local development).
     * Simulates processing delay, then creates mock outputs.
     */
    private CompletableFuture<TranscodingJobResult> mockTranscodingJob(
            UUID videoId, String s3Key) {

        log.info("🎭 MOCK: Creating transcoding job for video: {}", videoId);
        log.info("🎭 MOCK: Simulating {} seconds of processing...", mockProcessingSeconds);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate processing delay
                Thread.sleep(mockProcessingSeconds * 1000L);

                // Generate mock output keys
                String jobId = "MOCK-JOB-" + UUID.randomUUID().toString().substring(0, 8);
                String baseKey = s3Key.replace("videos/", "processed/");

                Map<String, String> qualityUrls = new HashMap<>();
                qualityUrls.put("360p", generateMockOutputKey(baseKey, "360p"));
                qualityUrls.put("720p", generateMockOutputKey(baseKey, "720p"));
                qualityUrls.put("1080p", generateMockOutputKey(baseKey, "1080p"));

                // In mock mode, we just reference the original video for all qualities
                // In production, MediaConvert actually creates these files
                for (Map.Entry<String, String> entry : qualityUrls.entrySet()) {
                    log.info("🎭 MOCK: Created {} output: {}", entry.getKey(), entry.getValue());
                }

                // Mock duration (you'd extract this from video metadata in real scenario)
                int mockDuration = 300; // 5 minutes placeholder

                log.info("✅ MOCK: Transcoding job completed: {}", jobId);

                return TranscodingJobResult.builder()
                        .videoId(videoId)
                        .jobId(jobId)
                        .originalS3Key(s3Key)
                        .qualityOutputs(qualityUrls)
                        .durationSeconds(mockDuration)
                        .status(JobStatus.COMPLETE)
                        .build();

            } catch (InterruptedException e) {
                log.error("❌ MOCK: Transcoding simulation interrupted", e);
                Thread.currentThread().interrupt();
                return TranscodingJobResult.failed(videoId, s3Key, "Mock interrupted");
            }
        });
    }

    /**
     * Real AWS MediaConvert transcoding job.
     * Submits job to AWS and returns immediately (job processes asynchronously).
     */
    private CompletableFuture<TranscodingJobResult> realTranscodingJob(
            UUID videoId, String s3Key) {

        log.info("🎬 AWS: Creating MediaConvert job for video: {}", videoId);
        log.info("🎬 AWS: Input S3 key: {}", s3Key);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate MediaConvert client is available
                if (mediaConvertClient == null) {
                    log.error("❌ MediaConvert client not initialized - check configuration");
                    return TranscodingJobResult.failed(videoId, s3Key,
                            "MediaConvert client not available");
                }

                // Validate output bucket is configured
                if (outputBucket == null || outputBucket.isEmpty()) {
                    log.error("❌ MediaConvert output bucket not configured");
                    log.error("❌ Set aws.mediaconvert.output-bucket in application.yml");
                    return TranscodingJobResult.failed(videoId, s3Key,
                            "Output bucket not configured");
                }

                // Validate IAM role is configured
                if (roleArn == null || roleArn.isEmpty()) {
                    log.error("❌ MediaConvert IAM role ARN not configured");
                    log.error("❌ Set aws.mediaconvert.role-arn in application.yml");
                    return TranscodingJobResult.failed(videoId, s3Key,
                            "IAM role ARN not configured");
                }

                // Build MediaConvert job settings
                CreateJobRequest request = buildJobRequest(s3Key);

                // Submit job to MediaConvert
                log.info("🎬 AWS: Submitting job to MediaConvert...");
                CreateJobResult result = mediaConvertClient.createJob(request);
                String jobId = result.getJob().getId();

                log.info("✅ AWS: MediaConvert job created successfully");
                log.info("✅ AWS: Job ID: {}", jobId);
                log.info("✅ AWS: Job will process asynchronously (webhook notification on completion)");

                // Job will complete asynchronously
                // We'll receive webhook notification when done
                return TranscodingJobResult.builder()
                        .videoId(videoId)
                        .jobId(jobId)
                        .originalS3Key(s3Key)
                        .status(JobStatus.SUBMITTED)
                        .build();

            } catch (AmazonServiceException e) {
                log.error("❌ AWS: MediaConvert API error", e);
                log.error("❌ AWS: Status Code: {}", e.getStatusCode());
                log.error("❌ AWS: Error Code: {}", e.getErrorCode());
                log.error("❌ AWS: Error Message: {}", e.getErrorMessage());
                return TranscodingJobResult.failed(videoId, s3Key,
                        "AWS error: " + e.getErrorMessage());

            } catch (Exception e) {
                log.error("❌ AWS: Failed to create MediaConvert job", e);
                return TranscodingJobResult.failed(videoId, s3Key,
                        "Unexpected error: " + e.getMessage());
            }
        });
    }

    /**
     * Build MediaConvert job request with output settings.
     */
    private CreateJobRequest buildJobRequest(String s3Key) {
        String inputPath = String.format("s3://%s/%s", inputBucket, s3Key);
        String outputPath = String.format("s3://%s/processed/", outputBucket);

        // Create output groups for each quality
        List<OutputGroup> outputGroups = Arrays.asList(
                createOutputGroup(outputPath, "360p", 640, 360, 800000),
                createOutputGroup(outputPath, "720p", 1280, 720, 2500000),
                createOutputGroup(outputPath, "1080p", 1920, 1080, 5000000)
        );

        JobSettings jobSettings = new JobSettings()
                .withOutputGroups(outputGroups)
                .withInputs(new Input().withFileInput(inputPath));

        CreateJobRequest request = new CreateJobRequest()
                .withRole(roleArn)
                .withSettings(jobSettings);

        // Use specific queue if provided
        if (queueArn != null && !queueArn.isEmpty()) {
            request.withQueue(queueArn);
        }

        return request;
    }

    /**
     * Create output group for specific quality.
     */
    private OutputGroup createOutputGroup(String outputPath, String quality,
                                          int width, int height, int bitrate) {

        Output output = new Output()
                .withNameModifier("_" + quality)
                .withVideoDescription(new VideoDescription()
                        .withWidth(width)
                        .withHeight(height)
                        .withCodecSettings(new VideoCodecSettings()
                                .withCodec(VideoCodec.H_264)
                                .withH264Settings(new H264Settings()
                                        .withBitrate(bitrate))));

        return new OutputGroup()
                .withName(quality)
                .withOutputGroupSettings(new OutputGroupSettings()
                        .withType(OutputGroupType.FILE_GROUP_SETTINGS)
                        .withFileGroupSettings(new FileGroupSettings()
                                .withDestination(outputPath + quality + "/")))
                .withOutputs(output);
    }

    /**
     * Generate mock output S3 key.
     */
    private String generateMockOutputKey(String baseKey, String quality) {
        return baseKey.replace(".mp4", "_" + quality + ".mp4");
    }

    /**
     * Check transcoding job status (for polling).
     */
    public JobStatus getJobStatus(String jobId) {
        if (useMock) {
            // Mock jobs complete instantly after delay
            return JobStatus.COMPLETE;
        }

        try {
            GetJobRequest request = new GetJobRequest().withId(jobId);
            GetJobResult result = mediaConvertClient.getJob(request);

            return mapJobStatus(result.getJob().getStatus());

        } catch (Exception e) {
            log.error("❌ Failed to get job status: {}", jobId, e);
            return JobStatus.ERROR;
        }
    }

    /**
     * Map AWS MediaConvert status to our enum.
     */
    private JobStatus mapJobStatus(String awsStatus) {
        switch (awsStatus) {
            case "SUBMITTED":
            case "PROGRESSING":
                return JobStatus.PROCESSING;
            case "COMPLETE":
                return JobStatus.COMPLETE;
            case "ERROR":
            case "CANCELED":
                return JobStatus.ERROR;
            default:
                return JobStatus.SUBMITTED;
        }
    }

    /**
     * Transcoding job result.
     */
    @lombok.Data
    @lombok.Builder
    public static class TranscodingJobResult {
        private UUID videoId;
        private String jobId;
        private String originalS3Key;
        private Map<String, String> qualityOutputs;
        private Integer durationSeconds;
        private JobStatus status;
        private String errorMessage;

        public static TranscodingJobResult disabled(UUID videoId, String s3Key) {
            return TranscodingJobResult.builder()
                    .videoId(videoId)
                    .originalS3Key(s3Key)
                    .status(JobStatus.DISABLED)
                    .build();
        }

        public static TranscodingJobResult failed(UUID videoId, String s3Key, String error) {
            return TranscodingJobResult.builder()
                    .videoId(videoId)
                    .originalS3Key(s3Key)
                    .status(JobStatus.ERROR)
                    .errorMessage(error)
                    .build();
        }
    }

    /**
     * Job status enum.
     */
    public enum JobStatus {
        SUBMITTED,
        PROCESSING,
        COMPLETE,
        ERROR,
        DISABLED
    }
}