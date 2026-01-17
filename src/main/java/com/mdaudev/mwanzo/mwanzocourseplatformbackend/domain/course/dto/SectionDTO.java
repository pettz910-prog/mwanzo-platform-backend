package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Section DTO with videos
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionDTO {

    private UUID id;
    private String title;
    private String description;
    private Integer displayOrder;
    private Integer totalDurationMinutes;
    private Integer videoCount;
    private List<VideoDTO> videos;
}