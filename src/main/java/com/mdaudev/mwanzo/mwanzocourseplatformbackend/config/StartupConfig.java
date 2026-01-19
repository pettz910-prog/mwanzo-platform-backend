package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.S3Service;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Startup Configuration
 *
 * Initializes services when application starts.
 * AWS clients are now initialized via AwsConfig beans.
 *
 * @author Mwanzo Development Team
 * @version 2.0
 * @since 2026-01-18
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class StartupConfig {

    private final S3Service s3Service;

    @PostConstruct
    public void init() {
        log.info("🚀 Initializing Mwanzo Course Platform...");
        log.info("🚀 AWS clients initialized via AwsConfig beans");

        // Initialize S3 bucket (create if needed in dev, verify access in prod)
        try {
            s3Service.initializeBucket();
            log.info("✅ S3 bucket ready");
        } catch (Exception e) {
            log.error("❌ Failed to initialize S3 bucket", e);
            log.error("❌ Video upload features will not work");
            // Don't fail startup - allow app to run without video features
        }

        log.info("✅ Platform initialization complete");
    }
}