package com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.JobStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.JobType;
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
public class JobDTO {
    private UUID id;
    private String title;
    private UUID companyId;
    private String location;
    private JobType type;
    private Integer salaryMin;
    private Integer salaryMax;
    private String description;
    private String responsibilities;
    private String requirements;
    private List<String> skills;
    private List<UUID> requiredCourseIds;
    private LocalDateTime postedAt;
    private LocalDateTime expiresAt;
    private JobStatus status;
}