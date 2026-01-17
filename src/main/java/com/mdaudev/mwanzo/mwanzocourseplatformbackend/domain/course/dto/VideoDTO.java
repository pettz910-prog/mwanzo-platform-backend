package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Video DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoDTO {

    private UUID id;
    private UUID sectionId;
    private String title;
    private String description;
    private Integer displayOrder;
    private Integer durationSeconds;
    private String videoUrl;
    private String streamingUrl;
    private String thumbnailUrl;
    private Boolean isPreview;
    private Boolean isCompleted;  // For enrolled students
    private Integer progressPercentage;  // For enrolled students
    private Integer lastPositionSeconds;  // For resume playback
    private String processingStatus;
}