package com.mdaudev.mwanzo.mwanzocourseplatformbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Async Configuration
 *
 * Enables @Async annotation for asynchronous method execution.
 * Used by MediaConvertService for non-blocking transcoding jobs.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-17
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot auto-configures thread pool from application.yml
    // spring.task.execution.pool settings
}