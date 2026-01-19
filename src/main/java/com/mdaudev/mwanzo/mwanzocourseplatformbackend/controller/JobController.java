package com.mdaudev.mwanzo.mwanzocourseplatformbackend.controller;

import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto.CreateJobRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto.JobDTO;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.domain.job.dto.UpdateJobRequest;
import com.mdaudev.mwanzo.mwanzocourseplatformbackend.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobDTO> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestParam UUID companyId) {
        log.info("POST /api/v1/jobs - Creating job for company: {}", companyId);
        JobDTO job = jobService.createJob(request, companyId);
        return ResponseEntity.status(HttpStatus.CREATED).body(job);
    }

    @GetMapping
    public ResponseEntity<Page<JobDTO>> getAllJobs(Pageable pageable) {
        log.info("GET /api/v1/jobs - Fetching all jobs");
        return ResponseEntity.ok(jobService.getAllJobs(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<Page<JobDTO>> getActiveJobs(Pageable pageable) {
        log.info("GET /api/v1/jobs/active - Fetching active jobs");
        return ResponseEntity.ok(jobService.getActiveJobs(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJobById(@PathVariable UUID id) {
        log.info("GET /api/v1/jobs/{} - Fetching job by ID", id);
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<JobDTO>> getJobsByCompany(
            @PathVariable UUID companyId,
            Pageable pageable) {
        log.info("GET /api/v1/jobs/company/{} - Fetching jobs for company", companyId);
        return ResponseEntity.ok(jobService.getJobsByCompany(companyId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDTO> updateJob(
            @PathVariable UUID id,
            @RequestBody UpdateJobRequest request) {
        log.info("PUT /api/v1/jobs/{} - Updating job", id);
        return ResponseEntity.ok(jobService.updateJob(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        log.info("DELETE /api/v1/jobs/{} - Deleting job", id);
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}