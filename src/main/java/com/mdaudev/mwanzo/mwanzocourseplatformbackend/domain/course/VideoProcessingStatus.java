package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course;

/**
 * Video Processing Status Enum
 *
 * Tracks video transcoding/processing lifecycle.
 *
 * @author Mwanzo Development Team
 * @version 1.0
 * @since 2026-01-12
 */
public enum VideoProcessingStatus {
    /**
     * Video uploaded to S3, not yet transcoded.
     */
    UPLOADED,

    /**
     * Video being transcoded by AWS MediaConvert.
     */
    PROCESSING,

    /**
     * Video ready for streaming to students.
     */
    READY,

    /**
     * Transcoding failed - needs retry or re-upload.
     */
    FAILED
}