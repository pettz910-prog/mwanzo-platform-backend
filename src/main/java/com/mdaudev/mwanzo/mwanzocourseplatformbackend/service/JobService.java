package com.mdaudev.mwanzo.mwanzocourseplatformbackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.Job;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.JobStatus;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto.CreateJobRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto.JobDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto.UpdateJobRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.exception.ResourceNotFoundException;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobDTO createJob(CreateJobRequest request, UUID companyId) {
        log.info("Creating job: {} for company: {}", request.getTitle(), companyId);

        Job job = Job.builder()
                .title(request.getTitle())
                .companyId(companyId)
                .location(request.getLocation())
                .type(request.getType())
                .salaryMin(request.getSalaryMin())
                .salaryMax(request.getSalaryMax())
                .description(request.getDescription())
                .responsibilities(request.getResponsibilities())
                .requirements(request.getRequirements())
                .skills(serializeList(request.getSkills()))
                .requiredCourseIds(serializeUUIDList(request.getRequiredCourseIds()))
                .expiresAt(request.getExpiresAt())
                .status(JobStatus.ACTIVE)
                .build();

        Job savedJob = jobRepository.save(job);
        log.info("Job created successfully with ID: {}", savedJob.getId());

        return mapToDTO(savedJob);
    }

    public Page<JobDTO> getAllJobs(Pageable pageable) {
        return jobRepository.findAll(pageable).map(this::mapToDTO);
    }

    public Page<JobDTO> getActiveJobs(Pageable pageable) {
        return jobRepository.findByStatus(JobStatus.ACTIVE, pageable).map(this::mapToDTO);
    }

    public JobDTO getJobById(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));
        return mapToDTO(job);
    }

    public Page<JobDTO> getJobsByCompany(UUID companyId, Pageable pageable) {
        return jobRepository.findByCompanyId(companyId, pageable).map(this::mapToDTO);
    }

    @Transactional
    public JobDTO updateJob(UUID id, UpdateJobRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + id));

        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getType() != null) job.setType(request.getType());
        if (request.getSalaryMin() != null) job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) job.setSalaryMax(request.getSalaryMax());
        if (request.getDescription() != null) job.setDescription(request.getDescription());
        if (request.getResponsibilities() != null) job.setResponsibilities(request.getResponsibilities());
        if (request.getRequirements() != null) job.setRequirements(request.getRequirements());
        if (request.getSkills() != null) job.setSkills(serializeList(request.getSkills()));
        if (request.getRequiredCourseIds() != null) job.setRequiredCourseIds(serializeUUIDList(request.getRequiredCourseIds()));
        if (request.getExpiresAt() != null) job.setExpiresAt(request.getExpiresAt());
        if (request.getStatus() != null) job.setStatus(request.getStatus());

        Job updatedJob = jobRepository.save(job);
        log.info("Job updated successfully: {}", updatedJob.getId());

        return mapToDTO(updatedJob);
    }

    @Transactional
    public void deleteJob(UUID id) {
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found with ID: " + id);
        }
        jobRepository.deleteById(id);
        log.info("Job deleted successfully: {}", id);
    }

    // Helper methods for JSON serialization
    private String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error serializing list", e);
            return "[]";
        }
    }

    private String serializeUUIDList(List<UUID> list) {
        if (list == null || list.isEmpty()) return "[]";
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Error serializing UUID list", e);
            return "[]";
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error deserializing list", e);
            return Collections.emptyList();
        }
    }

    private List<UUID> deserializeUUIDList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error deserializing UUID list", e);
            return Collections.emptyList();
        }
    }

    private JobDTO mapToDTO(Job job) {
        return JobDTO.builder()
                .id(job.getId())
                .title(job.getTitle())
                .companyId(job.getCompanyId())
                .location(job.getLocation())
                .type(job.getType())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .description(job.getDescription())
                .responsibilities(job.getResponsibilities())
                .requirements(job.getRequirements())
                .skills(deserializeList(job.getSkills()))
                .requiredCourseIds(deserializeUUIDList(job.getRequiredCourseIds()))
                .postedAt(job.getPostedAt())
                .expiresAt(job.getExpiresAt())
                .status(job.getStatus())
                .build();
    }
}