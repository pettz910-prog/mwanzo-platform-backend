package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {

    @NotBlank(message = "Job title is required")
    private String title;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Job type is required")
    private JobType type;

    private Integer salaryMin;
    private Integer salaryMax;

    @NotBlank(message = "Description is required")
    private String description;

    private String responsibilities;
    private String requirements;
    private List<String> skills;
    private List<UUID> requiredCourseIds;
    private LocalDateTime expiresAt;
}